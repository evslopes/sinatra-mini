package pro.sinatra.mini.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pro.sinatra.mini.repository.PedidoRepository;
import pro.sinatra.mini.reports.MediaSemanalDTO;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MediaSemanalServiceTest {

    @Mock
    private PedidoRepository pedidoRepository;

    @InjectMocks
    private MediaSemanalService service;

    private PedidoRepository.DailySalesProjection dailySale(LocalDate dia, double total) {
        return new PedidoRepository.DailySalesProjection() {
            public LocalDate getDia() { return dia; }
            public Double getTotal() { return total; }
        };
    }

    @Test
    public void calcularMediaSemanal_returnsAllSevenDays() {
        Long lojaId = 1L;
        LocalDate start = LocalDate.of(2025, 1, 1);
        LocalDate end = LocalDate.of(2025, 1, 31);

        // Mock data: 2025-01-01 is Wednesday
        var stream = Stream.of(
                dailySale(LocalDate.of(2025, 1, 1), 100.0),  // Wed
                dailySale(LocalDate.of(2025, 1, 2), 200.0),  // Thu
                dailySale(LocalDate.of(2025, 1, 8), 150.0),  // Wed
                dailySale(LocalDate.of(2025, 1, 9), 250.0)   // Thu
        );

        when(pedidoRepository.streamDailySalesByLojaBetween(lojaId, start, end))
                .thenReturn(stream);

        List<MediaSemanalDTO> result = service.calcularMediaSemanal(lojaId, start, end, ZoneId.of("UTC"));

        // Should return 7 days (Monday to Sunday)
        assertEquals(7, result.size());

        // Verify order: Monday=1, Tuesday=2, ..., Sunday=7
        for (int i = 0; i < 7; i++) {
            assertEquals(i + 1, result.get(i).dayOfWeek());
        }
    }

    @Test
    public void calcularMediaSemanal_calculatesCorrectAverages() {
        Long lojaId = 1L;
        LocalDate start = LocalDate.of(2025, 1, 1);
        LocalDate end = LocalDate.of(2025, 1, 31);

        // 2025-01-01 is Wednesday (DayOfWeek.WEDNESDAY = 3)
        // 2025-01-08 is also Wednesday
        var stream = Stream.of(
                dailySale(LocalDate.of(2025, 1, 1), 100.0),  // Wed
                dailySale(LocalDate.of(2025, 1, 8), 200.0)   // Wed
        );

        when(pedidoRepository.streamDailySalesByLojaBetween(lojaId, start, end))
                .thenReturn(stream);

        List<MediaSemanalDTO> result = service.calcularMediaSemanal(lojaId, start, end, ZoneId.of("UTC"));

        // Wednesday is day 3 (index 2 in result list)
        MediaSemanalDTO wednesday = result.get(2);
        assertEquals(3, wednesday.dayOfWeek()); // Wednesday
        assertEquals(150.0, wednesday.average(), 0.01); // (100 + 200) / 2

        // Other days should have 0.0
        MediaSemanalDTO monday = result.get(0);
        assertEquals(0.0, monday.average(), 0.01);
    }

    @Test
    public void calcularMediaSemanal_handlesEmptyStream() {
        Long lojaId = 1L;
        LocalDate start = LocalDate.of(2025, 1, 1);
        LocalDate end = LocalDate.of(2025, 1, 31);

        when(pedidoRepository.streamDailySalesByLojaBetween(lojaId, start, end))
                .thenReturn(Stream.empty());

        List<MediaSemanalDTO> result = service.calcularMediaSemanal(lojaId, start, end, ZoneId.of("UTC"));

        // Should still return 7 days with 0.0 averages
        assertEquals(7, result.size());
        result.forEach(dto -> assertEquals(0.0, dto.average(), 0.01));
    }

    @Test
    public void calcularMediaSemanal_handlesNullZone() {
        Long lojaId = 1L;
        LocalDate start = LocalDate.of(2025, 1, 1);
        LocalDate end = LocalDate.of(2025, 1, 31);

        var stream = Stream.of(
                dailySale(LocalDate.of(2025, 1, 1), 100.0)
        );

        when(pedidoRepository.streamDailySalesByLojaBetween(lojaId, start, end))
                .thenReturn(stream);

        // Should default to UTC when zone is null
        List<MediaSemanalDTO> result = service.calcularMediaSemanal(lojaId, start, end, null);

        assertEquals(7, result.size());
        assertNotNull(result);
    }

    @Test
    public void calcularMediaSemanal_handlesMultipleDaysPerWeekday() {
        Long lojaId = 1L;
        LocalDate start = LocalDate.of(2025, 1, 1);
        LocalDate end = LocalDate.of(2025, 1, 31);

        // Multiple Mondays with different totals
        // 2025-01-06, 2025-01-13, 2025-01-20, 2025-01-27 are Mondays
        var stream = Stream.of(
                dailySale(LocalDate.of(2025, 1, 6), 100.0),   // Mon
                dailySale(LocalDate.of(2025, 1, 13), 200.0),  // Mon
                dailySale(LocalDate.of(2025, 1, 20), 300.0),  // Mon
                dailySale(LocalDate.of(2025, 1, 27), 400.0)   // Mon
        );

        when(pedidoRepository.streamDailySalesByLojaBetween(lojaId, start, end))
                .thenReturn(stream);

        List<MediaSemanalDTO> result = service.calcularMediaSemanal(lojaId, start, end, ZoneId.of("UTC"));

        // Monday is day 1 (index 0)
        MediaSemanalDTO monday = result.get(0);
        assertEquals(1, monday.dayOfWeek());
        assertEquals(250.0, monday.average(), 0.01); // (100+200+300+400) / 4
    }

    @Test
    public void calcularMediaSemanal_withDifferentTimezone() {
        Long lojaId = 1L;
        LocalDate start = LocalDate.of(2025, 1, 1);
        LocalDate end = LocalDate.of(2025, 1, 31);

        var stream = Stream.of(
                dailySale(LocalDate.of(2025, 1, 1), 100.0)
        );

        when(pedidoRepository.streamDailySalesByLojaBetween(lojaId, start, end))
                .thenReturn(stream);

        // Test with São Paulo timezone
        List<MediaSemanalDTO> result = service.calcularMediaSemanal(
                lojaId, start, end, ZoneId.of("America/Sao_Paulo"));

        assertEquals(7, result.size());
        // Verify that calculation completes without errors
        assertNotNull(result);
    }
}
