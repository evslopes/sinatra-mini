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
	    @Query("SELECT cast(p.dataPedido as date) as dia, SUM(p.total) as total " +
		    "FROM Pedido p WHERE p.loja.id = :lojaId " +
		    "GROUP BY cast(p.dataPedido as date) " +
		    "ORDER BY cast(p.dataPedido as date)")
	Stream<DailySalesProjection> streamDailySalesByLoja(@Param("lojaId") Long lojaId);
}