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

	// Projection for distinct sale days (used by Story 2.2)
	interface DailySaleDaysProjection {
		java.time.LocalDate getDia();
	}

	// Stream distinct days with sales for a loja within optional date bounds
	@Query("SELECT DISTINCT cast(p.dataPedido as date) as dia " +
			"FROM Pedido p WHERE p.loja.id = :lojaId " +
			"AND (:start IS NULL OR cast(p.dataPedido as date) >= :start) " +
			"AND (:end IS NULL OR cast(p.dataPedido as date) <= :end) " +
			"ORDER BY dia")
	Stream<DailySaleDaysProjection> streamDistinctSaleDaysByLoja(
			@Param("lojaId") Long lojaId,
			@Param("start") java.time.LocalDate start,
			@Param("end") java.time.LocalDate end);
}