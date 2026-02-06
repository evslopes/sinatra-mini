package pro.sinatra.mini.service;

import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.transaction.annotation.Transactional;
import pro.sinatra.mini.repository.PedidoRepository;
import pro.sinatra.mini.reports.MediaSemanalDTO;

import java.time.*;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MediaSemanalService {

    private final PedidoRepository pedidoRepository;

    public MediaSemanalService(PedidoRepository pedidoRepository) {
        this.pedidoRepository = pedidoRepository;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "mediaSemanal", key = "#lojaId + '-' + #start + '-' + #end + '-' + #zone")
    public List<MediaSemanalDTO> calcularMediaSemanal(Long lojaId, LocalDate start, LocalDate end, ZoneId zone) {
        ZoneId targetZone = (zone == null) ? ZoneId.of("UTC") : zone;

        // Initialize accumulators for DayOfWeek MON..SUN
        Map<DayOfWeek, double[]> acc = new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek d : DayOfWeek.values()) {
            acc.put(d, new double[]{0.0, 0.0}); // [0]=total, [1]=count
        }

        try (var stream = pedidoRepository.streamDailySalesByLojaBetween(lojaId, start, end)) {
            stream.forEach(daily -> {
                LocalDate dia = daily.getDia();
                if (dia == null) return;
                // interpret stored date (db date) as local date at start of day in UTC, then convert to target zone
                LocalDateTime ldt = dia.atStartOfDay();
                Instant instant = ldt.atOffset(ZoneOffset.UTC).toInstant();
                DayOfWeek dow = instant.atZone(targetZone).getDayOfWeek();
                double total = daily.getTotal() == null ? 0.0 : daily.getTotal();
                double[] vals = acc.get(dow);
                vals[0] += total;
                vals[1] += 1.0;
            });
        }

        // Build result list Monday..Sunday
        return java.util.Arrays.stream(DayOfWeek.values())
                .map(d -> {
                    double[] v = acc.get(d);
                    double avg = (v[1] == 0.0) ? 0.0 : (v[0] / v[1]);
                    return new MediaSemanalDTO(d.getValue(), Math.round(avg * 100.0) / 100.0);
                })
                .collect(Collectors.toList());
    }
}
