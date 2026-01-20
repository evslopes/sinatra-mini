package pro.sinatra.mini.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pro.sinatra.mini.repository.PedidoRepository;
import pro.sinatra.mini.reports.MenorVendaDiaDTO;

import java.time.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
public class DiaMenorVendaService {

    private final PedidoRepository pedidoRepository;

    public DiaMenorVendaService(PedidoRepository pedidoRepository) {
        this.pedidoRepository = pedidoRepository;
    }

    @Transactional(readOnly = true)
    public Optional<MenorVendaDiaDTO> calcularDiaMenorVenda(Long lojaId, LocalDate start, LocalDate end, ZoneId zone) {
        Objects.requireNonNull(lojaId, "lojaId must not be null");
        if (zone == null) zone = ZoneId.of("UTC");

        LocalDateTime startDt = (start == null) ? null : start.atStartOfDay();
        LocalDateTime endDt = (end == null) ? null : end.atTime(LocalTime.MAX);

        Map<LocalDate, Double> totals = new HashMap<>();

        try (var stream = pedidoRepository.streamSalesByLoja(lojaId, startDt, endDt)) {
            stream.forEach(s -> {
                java.time.LocalDateTime ldt = s.getDataPedido();
                if (ldt == null) return;
                // Interpret stored LocalDateTime as UTC instant, then convert to target zone
                Instant instant = ldt.atOffset(ZoneOffset.UTC).toInstant();
                LocalDate localDay = instant.atZone(zone).toLocalDate();
                totals.merge(localDay, s.getTotal() == null ? 0.0 : s.getTotal(), Double::sum);
            });
        }

        return totals.entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .map(e -> new MenorVendaDiaDTO(e.getKey(), e.getValue()));
    }
}
