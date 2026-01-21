package pro.sinatra.mini.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pro.sinatra.mini.repository.PedidoRepository;
import pro.sinatra.mini.reports.MenorVendaDiaDTO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DiaMenorVendaServiceTest {

    @Mock
    private PedidoRepository pedidoRepository;

    @InjectMocks
    private DiaMenorVendaService service;

    private PedidoRepository.SaleProjection sale(LocalDateTime dt, double total) {
        return new PedidoRepository.SaleProjection() {
            public LocalDateTime getDataPedido() { return dt; }
            public Double getTotal() { return total; }
        };
    }

    @Test
    public void calcularDiaMenorVenda_returnsExpectedDay() {
        Long lojaId = 1L;
        // Dates in UTC
        var s = Stream.of(
                sale(LocalDateTime.of(2025,1,1,10,0), 100.0),
                sale(LocalDateTime.of(2025,1,2,12,0), 50.0),
                sale(LocalDateTime.of(2025,1,2,15,0), 20.0),
                sale(LocalDateTime.of(2025,1,3,9,0), 200.0)
        );
        when(pedidoRepository.streamSalesByLoja(lojaId, null, null)).thenReturn(s);

        Optional<MenorVendaDiaDTO> opt = service.calcularDiaMenorVenda(lojaId, null, null, ZoneId.of("UTC"));
        assertTrue(opt.isPresent());
        MenorVendaDiaDTO dto = opt.get();
        assertEquals(LocalDate.of(2025,1,2), dto.dia());
        assertEquals(70.0, dto.total(), 0.01);
    }
}
