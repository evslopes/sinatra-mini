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

@RestController
@RequestMapping("/pedidos")
public class PedidoController {
    @Autowired
    private PedidoService pedidoService;

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
}