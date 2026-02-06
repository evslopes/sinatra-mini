package pro.sinatra.mini.service;

import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.transaction.annotation.Transactional;
import pro.sinatra.mini.repository.PedidoRepository;
import pro.sinatra.mini.reports.PeriodoDTO;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class MaiorIntervaloSemVendasService {

    private final PedidoRepository pedidoRepository;

    public MaiorIntervaloSemVendasService(PedidoRepository pedidoRepository) {
        this.pedidoRepository = pedidoRepository;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "maiorIntervalo", key = "#lojaId + '-' + #inicio + '-' + #fim")
    public PeriodoDTO calcularMaiorIntervalo(Long lojaId, LocalDate inicio, LocalDate fim) {
        Objects.requireNonNull(lojaId, "lojaId must not be null");

        // Collect ordered distinct sale days
        List<LocalDate> saleDays;
        try (var stream = pedidoRepository.streamDistinctSaleDaysByLoja(lojaId, inicio, fim)) {
            saleDays = stream.map(PedidoRepository.DailySaleDaysProjection::getDia)
                    .collect(Collectors.toList());
        }

        LocalDate windowStart = inicio;
        LocalDate windowEnd = fim;

        if (windowStart == null && !saleDays.isEmpty()) {
            windowStart = saleDays.get(0);
        }
        if (windowEnd == null && !saleDays.isEmpty()) {
            windowEnd = saleDays.get(saleDays.size() - 1);
        }

        if (windowStart == null && windowEnd == null) {
            // No bounds and no sales: treat as single-day window at today
            windowStart = LocalDate.now();
            windowEnd = windowStart;
        } else if (windowStart == null) {
            windowStart = windowEnd;
        } else if (windowEnd == null) {
            windowEnd = windowStart;
        }

        // If no sales in the window, entire window is no-sales
        if (saleDays.isEmpty()) {
            long days = ChronoUnit.DAYS.between(windowStart, windowEnd) + 1;
            return new PeriodoDTO(windowStart, windowEnd, Math.max(0, days));
        }

        // Ensure saleDays are within window bounds (they should be per query)
        List<LocalDate> filtered = new ArrayList<>();
        for (LocalDate d : saleDays) {
            if ((d.isEqual(windowStart) || d.isAfter(windowStart)) && (d.isEqual(windowEnd) || d.isBefore(windowEnd))) {
                filtered.add(d);
            }
        }
        if (filtered.isEmpty()) {
            long days = ChronoUnit.DAYS.between(windowStart, windowEnd) + 1;
            return new PeriodoDTO(windowStart, windowEnd, Math.max(0, days));
        }

        LocalDate bestStart = windowStart;
        LocalDate bestEnd = windowStart.minusDays(1);
        long bestDays = 0;

        // Gap before first sale
        LocalDate firstSale = filtered.get(0);
        if (firstSale.isAfter(windowStart)) {
            LocalDate gapStart = windowStart;
            LocalDate gapEnd = firstSale.minusDays(1);
            long gapDays = ChronoUnit.DAYS.between(gapStart, gapEnd) + 1;
            if (gapDays > bestDays) {
                bestDays = gapDays;
                bestStart = gapStart;
                bestEnd = gapEnd;
            }
        }

        // Gaps between consecutive sale days
        for (int i = 0; i < filtered.size() - 1; i++) {
            LocalDate prev = filtered.get(i);
            LocalDate next = filtered.get(i + 1);
            LocalDate gapStart = prev.plusDays(1);
            LocalDate gapEnd = next.minusDays(1);
            if (!gapEnd.isBefore(gapStart)) {
                long gapDays = ChronoUnit.DAYS.between(gapStart, gapEnd) + 1;
                if (gapDays > bestDays) {
                    bestDays = gapDays;
                    bestStart = gapStart;
                    bestEnd = gapEnd;
                }
            }
        }

        // Gap after last sale
        LocalDate lastSale = filtered.get(filtered.size() - 1);
        if (lastSale.isBefore(windowEnd)) {
            LocalDate gapStart = lastSale.plusDays(1);
            LocalDate gapEnd = windowEnd;
            long gapDays = ChronoUnit.DAYS.between(gapStart, gapEnd) + 1;
            if (gapDays > bestDays) {
                bestDays = gapDays;
                bestStart = gapStart;
                bestEnd = gapEnd;
            }
        }

        return new PeriodoDTO(bestStart, bestEnd, Math.max(0, bestDays));
    }
}
