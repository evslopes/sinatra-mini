package pro.sinatra.mini.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pro.sinatra.mini.entity.Pedido;
import pro.sinatra.mini.repository.PedidoRepository;

import java.util.List;
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
}