package pro.sinatra.mini.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pro.sinatra.mini.entity.Pedido;
import pro.sinatra.mini.repository.PedidoRepository;
import pro.sinatra.mini.reports.DailySalesDTO;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class PedidoService {
    @Autowired
    private PedidoRepository pedidoRepository;

    public List<Pedido> findAll() {
        return pedidoRepository.findAll();
    }

    public Pedido findById(Long id) {
        return pedidoRepository.findById(id).orElse(null);
    }

    public Pedido save(Pedido pedido) {
        return pedidoRepository.save(pedido);
    }

    public void deleteById(Long id) {
        pedidoRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Stream<PedidoRepository.DailySalesProjection> streamDailySalesByLoja(Long lojaId) {
        return pedidoRepository.streamDailySalesByLoja(lojaId);
    }

    // Cached helper that materializes the stream into a DTO list for safe caching
    @Transactional(readOnly = true)
    @Cacheable(value = "relatorios", key = "#lojaId")
    public List<DailySalesDTO> getDailySalesCached(Long lojaId) {
        try (Stream<PedidoRepository.DailySalesProjection> s = pedidoRepository.streamDailySalesByLoja(lojaId)) {
            return s.map(p -> new DailySalesDTO(p.getDia(), p.getTotal()))
                    .collect(Collectors.toList());
        }
    }
}