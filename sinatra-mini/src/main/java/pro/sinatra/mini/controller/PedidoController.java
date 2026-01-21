package pro.sinatra.mini.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;
import pro.sinatra.mini.entity.Pedido;
import pro.sinatra.mini.service.PedidoService;
import pro.sinatra.mini.service.MaiorIntervaloSemVendasService;
import pro.sinatra.mini.reports.PeriodoDTO;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.stream.Stream;
import pro.sinatra.mini.repository.PedidoRepository;
import pro.sinatra.mini.service.DiaMenorVendaService;
import pro.sinatra.mini.reports.MenorVendaDiaDTO;
<<<<<<< HEAD
import pro.sinatra.mini.service.MediaSemanalService;
=======
>>>>>>> origin/main
import java.time.LocalDate;
import java.time.ZoneId;

@RestController
@RequestMapping("/pedidos")
public class PedidoController {
    @Autowired
    private PedidoService pedidoService;

    @Autowired
    private MaiorIntervaloSemVendasService maiorIntervaloService;

    @Autowired
    private DiaMenorVendaService diaMenorVendaService;

<<<<<<< HEAD
    @Autowired
    private MediaSemanalService mediaSemanalService;

=======
>>>>>>> origin/main
    @GetMapping
    public List<Pedido> findAll() {
        return pedidoService.findAll();
    }

    @GetMapping("/{id}")
    public Pedido findById(@PathVariable Long id) {
        return pedidoService.findById(id);
    }

    @PostMapping
    public Pedido save(@RequestBody Pedido pedido) {
        return pedidoService.save(pedido);
    }

    @DeleteMapping("/{id}")
    public void deleteById(@PathVariable Long id) {
        pedidoService.deleteById(id);
    }

    @GetMapping(value = "/{lojaId}/reports/daily-sales.csv", produces = "text/csv")
    public ResponseEntity<StreamingResponseBody> exportDailySalesCsv(@PathVariable Long lojaId) {
        StreamingResponseBody body = outputStream -> {
            try (Stream<PedidoRepository.DailySalesProjection> stream = pedidoService.streamDailySalesByLoja(lojaId);
                 BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream))) {
                // CSV header
                writer.write("date,total\n");
                stream.forEach(p -> {
                    try {
                        writer.write(String.format("%s,%.2f\n", p.getDia().toString(), p.getTotal() == null ? 0.0 : p.getTotal()));
                        writer.flush();
                    } catch (IOException e) {
                        // stop streaming on IO error
                        throw new RuntimeException(e);
                    }
                });
                writer.flush();
            }
        };

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=daily-sales-loja-" + lojaId + ".csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(body);
    }

    @GetMapping("/{lojaId}/reports/longest-no-sales")
    public PeriodoDTO longestNoSales(
            @PathVariable Long lojaId,
            @RequestParam(name = "start", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate start,
            @RequestParam(name = "end", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate end) {
        return maiorIntervaloService.calcularMaiorIntervalo(lojaId, start, end);
    }

    @GetMapping("/{lojaId}/reports/min-day-sales")
    public ResponseEntity<MenorVendaDiaDTO> minDaySales(
            @PathVariable Long lojaId,
            @RequestParam(name = "start", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(name = "end", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(name = "zoneId", required = false) String zoneId) {
        ZoneId zone = (zoneId == null) ? ZoneId.of("UTC") : ZoneId.of(zoneId);
        return diaMenorVendaService.calcularDiaMenorVenda(lojaId, start, end, zone)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
<<<<<<< HEAD

    @GetMapping("/{lojaId}/reports/weekly-average")
    public java.util.List<pro.sinatra.mini.reports.MediaSemanalDTO> weeklyAverage(
            @PathVariable Long lojaId,
            @RequestParam(name = "start", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate start,
            @RequestParam(name = "end", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate end,
            @RequestParam(name = "zoneId", required = false) String zoneId) {
        java.time.ZoneId zone = (zoneId == null) ? java.time.ZoneId.of("UTC") : java.time.ZoneId.of(zoneId);
        return mediaSemanalService.calcularMediaSemanal(lojaId, start, end, zone);
    }
}
package pro.sinatra.mini.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import pro.sinatra.mini.entity.Pedido;
import pro.sinatra.mini.service.PedidoService;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.stream.Stream;
import pro.sinatra.mini.repository.PedidoRepository;
<<<<<<< Updated upstream
=======
import pro.sinatra.mini.service.DiaMenorVendaService;
import pro.sinatra.mini.reports.MenorVendaDiaDTO;
import pro.sinatra.mini.service.MediaSemanalService;
import java.time.LocalDate;
import java.time.ZoneId;
>>>>>>> Stashed changes

@RestController
@RequestMapping("/pedidos")
public class PedidoController {
    @Autowired
    private PedidoService pedidoService;

<<<<<<< Updated upstream
=======
    @Autowired
    private MaiorIntervaloSemVendasService maiorIntervaloService;

    @Autowired
    private DiaMenorVendaService diaMenorVendaService;
    @Autowired
    private MediaSemanalService mediaSemanalService;

>>>>>>> Stashed changes
    @GetMapping
    public List<Pedido> findAll() {
        return pedidoService.findAll();
    }

    @GetMapping("/{id}")
    public Pedido findById(@PathVariable Long id) {
        return pedidoService.findById(id);
    }

    @PostMapping
    public Pedido save(@RequestBody Pedido pedido) {
        return pedidoService.save(pedido);
    }

    @DeleteMapping("/{id}")
    public void deleteById(@PathVariable Long id) {
        pedidoService.deleteById(id);
    }

    @GetMapping(value = "/{lojaId}/reports/daily-sales.csv", produces = "text/csv")
    public ResponseEntity<StreamingResponseBody> exportDailySalesCsv(@PathVariable Long lojaId) {
        StreamingResponseBody body = outputStream -> {
            try (Stream<PedidoRepository.DailySalesProjection> stream = pedidoService.streamDailySalesByLoja(lojaId);
                 BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream))) {
                // CSV header
                writer.write("date,total\n");
                stream.forEach(p -> {
                    try {
                        writer.write(String.format("%s,%.2f\n", p.getDia().toString(), p.getTotal() == null ? 0.0 : p.getTotal()));
                        writer.flush();
                    } catch (IOException e) {
                        // stop streaming on IO error
                        throw new RuntimeException(e);
                    }
                });
                writer.flush();
            }
        };

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=daily-sales-loja-" + lojaId + ".csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(body);
    }
<<<<<<< Updated upstream
=======

    @GetMapping("/{lojaId}/reports/longest-no-sales")
    public PeriodoDTO longestNoSales(
            @PathVariable Long lojaId,
            @RequestParam(name = "start", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate start,
            @RequestParam(name = "end", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate end) {
        return maiorIntervaloService.calcularMaiorIntervalo(lojaId, start, end);
    }

    @GetMapping("/{lojaId}/reports/min-day-sales")
    public ResponseEntity<MenorVendaDiaDTO> minDaySales(
            @PathVariable Long lojaId,
            @RequestParam(name = "start", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(name = "end", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(name = "zoneId", required = false) String zoneId) {
        ZoneId zone = (zoneId == null) ? ZoneId.of("UTC") : ZoneId.of(zoneId);
        return diaMenorVendaService.calcularDiaMenorVenda(lojaId, start, end, zone)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/{lojaId}/reports/weekly-average")
    public java.util.List<pro.sinatra.mini.reports.MediaSemanalDTO> weeklyAverage(
            @PathVariable Long lojaId,
            @RequestParam(name = "start", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate start,
            @RequestParam(name = "end", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate end,
            @RequestParam(name = "zoneId", required = false) String zoneId) {
        java.time.ZoneId zone = (zoneId == null) ? java.time.ZoneId.of("UTC") : java.time.ZoneId.of(zoneId);
        return mediaSemanalService.calcularMediaSemanal(lojaId, start, end, zone);
    }
>>>>>>> Stashed changes
=======
>>>>>>> origin/main
}