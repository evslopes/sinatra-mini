package pro.sinatra.mini.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pro.sinatra.mini.repository.PedidoRepository;
import pro.sinatra.mini.reports.PeriodoDTO;

import java.time.LocalDate;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MaiorIntervaloSemVendasServiceTest {

    @Mock
    private PedidoRepository pedidoRepository;

    @InjectMocks
    private MaiorIntervaloSemVendasService service;

    private PedidoRepository.DailySaleDaysProjection day(LocalDate d) {
        return new PedidoRepository.DailySaleDaysProjection() {
            @Override
            public LocalDate getDia() {
                return d;
            }
        };
    }

    @Test
    public void calcularMaiorIntervalo_noSales_returnsFullWindow() {
        Long lojaId = 1L;
        LocalDate start = LocalDate.of(2025, 1, 1);
        LocalDate end = LocalDate.of(2025, 1, 10);
        when(pedidoRepository.streamDistinctSaleDaysByLoja(lojaId, start, end)).thenReturn(Stream.empty());

        PeriodoDTO dto = service.calcularMaiorIntervalo(lojaId, start, end);
        assertEquals(start, dto.dataInicial());
        assertEquals(end, dto.dataFinal());
        assertEquals(10, dto.dias());
    }

    @Test
    public void calcularMaiorIntervalo_findsLargestGap() {
        Long lojaId = 2L;
        LocalDate start = LocalDate.of(2025, 1, 1);
        LocalDate end = LocalDate.of(2025, 1, 15);

        // Sales on 2,3,7,14 => gaps: 1(day1), 4-6(3 days), 8-13(6 days)
        Stream<PedidoRepository.DailySaleDaysProjection> stream = Stream.of(
                day(LocalDate.of(2025,1,2)),
                day(LocalDate.of(2025,1,3)),
                day(LocalDate.of(2025,1,7)),
                day(LocalDate.of(2025,1,14))
        );
        when(pedidoRepository.streamDistinctSaleDaysByLoja(lojaId, start, end)).thenReturn(stream);

        PeriodoDTO dto = service.calcularMaiorIntervalo(lojaId, start, end);
        assertEquals(LocalDate.of(2025,1,8), dto.dataInicial());
        assertEquals(LocalDate.of(2025,1,13), dto.dataFinal());
        assertEquals(6, dto.dias());
    }
}
