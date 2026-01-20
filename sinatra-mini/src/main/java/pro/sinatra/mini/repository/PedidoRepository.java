package pro.sinatra.mini.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pro.sinatra.mini.entity.Pedido;

import java.util.stream.Stream;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

	// Projection interface for daily sales aggregation
	interface DailySalesProjection {
		java.time.LocalDate getDia();
		Double getTotal();
	}

	// Stream daily totals per date for a given loja (uses SQL DATE function)
	@Query("SELECT function('DATE', p.dataPedido) as dia, SUM(p.total) as total " +
		   "FROM Pedido p WHERE p.loja.id = :lojaId " +
		   "GROUP BY function('DATE', p.dataPedido) " +
		   "ORDER BY function('DATE', p.dataPedido)")
	Stream<DailySalesProjection> streamDailySalesByLoja(@Param("lojaId") Long lojaId);

	// Raw sales projection (dataPedido with time) used to compute day boundaries with ZoneId in Service
	interface SaleProjection {
		java.time.LocalDateTime getDataPedido();
		Double getTotal();
	}

	@Query("SELECT p.dataPedido as dataPedido, p.total as total " +
		   "FROM Pedido p WHERE p.loja.id = :lojaId " +
		   "AND (:start IS NULL OR p.dataPedido >= :start) " +
		   "AND (:end IS NULL OR p.dataPedido <= :end) " +
		   "ORDER BY p.dataPedido")
	Stream<SaleProjection> streamSalesByLoja(@Param("lojaId") Long lojaId,
											@Param("start") java.time.LocalDateTime start,
											@Param("end") java.time.LocalDateTime end);
}