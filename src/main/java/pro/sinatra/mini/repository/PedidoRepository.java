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

    // Stream daily totals per date for a given loja within optional date bounds
	@Query(value = "SELECT CAST(p.data_pedido AS date) AS dia, SUM(p.total) AS total " +
		"FROM pedido p WHERE p.loja_id = :lojaId " +
		"AND (CAST(:start AS date) IS NULL OR CAST(p.data_pedido AS date) >= CAST(:start AS date)) " +
		"AND (CAST(:end AS date) IS NULL OR CAST(p.data_pedido AS date) <= CAST(:end AS date)) " +
		"GROUP BY CAST(p.data_pedido AS date) " +
		"ORDER BY CAST(p.data_pedido AS date)", nativeQuery = true)
	Stream<DailySalesProjection> streamDailySalesByLojaBetween(
		@Param("lojaId") Long lojaId,
		@Param("start") java.time.LocalDate start,
		@Param("end") java.time.LocalDate end);

    // Projection for distinct sale days (used by Story 2.2)
    interface DailySaleDaysProjection {
	java.time.LocalDate getDia();
    }

    // Stream distinct days with sales for a loja within optional date bounds
	@Query(value = "SELECT DISTINCT CAST(p.data_pedido AS date) AS dia " +
		"FROM pedido p WHERE p.loja_id = :lojaId " +
		"AND (CAST(:start AS date) IS NULL OR CAST(p.data_pedido AS date) >= CAST(:start AS date)) " +
		"AND (CAST(:end AS date) IS NULL OR CAST(p.data_pedido AS date) <= CAST(:end AS date)) " +
		"ORDER BY dia", nativeQuery = true)
	Stream<DailySaleDaysProjection> streamDistinctSaleDaysByLoja(
		@Param("lojaId") Long lojaId,
		@Param("start") java.time.LocalDate start,
		@Param("end") java.time.LocalDate end);

    // Projection for raw sales with timestamp and total (used by Story 2.3)
    interface SaleProjection {
	java.time.LocalDateTime getDataPedido();
	Double getTotal();
    }

    // Stream sales with timestamps for a loja within optional date-time bounds
	@Query(value = "SELECT p.data_pedido AS dataPedido, p.total AS total " +
		"FROM pedido p WHERE p.loja_id = :lojaId " +
		"AND (CAST(:start AS timestamp) IS NULL OR p.data_pedido >= CAST(:start AS timestamp)) " +
		"AND (CAST(:end AS timestamp) IS NULL OR p.data_pedido <= CAST(:end AS timestamp)) " +
		"ORDER BY p.data_pedido", nativeQuery = true)
	Stream<SaleProjection> streamSalesByLoja(
		@Param("lojaId") Long lojaId,
		@Param("start") java.time.LocalDateTime start,
		@Param("end") java.time.LocalDateTime end);
}