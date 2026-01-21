package pro.sinatra.mini.reports;

import java.time.LocalDate;

public record PeriodoDTO(LocalDate dataInicial, LocalDate dataFinal, long dias) {
}
