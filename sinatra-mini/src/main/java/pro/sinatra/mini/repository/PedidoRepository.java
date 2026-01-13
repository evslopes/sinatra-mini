package pro.sinatra.mini.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pro.sinatra.mini.entity.Pedido;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
}