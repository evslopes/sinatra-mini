package pro.sinatra.mini.test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import org.h2.tools.TriggerAdapter;
import java.sql.ResultSet;

/**
 * H2 fallback equivalent of PostgreSQL migration V11__ensure_total_from_valor_total.sql.
 * Keeps total, valor_total, and numero_pedido synchronized regardless of whether rows
 * are inserted via JPA entities or raw SQL fixtures.
 */
public class H2PedidoTotalSyncTrigger extends TriggerAdapter {

    @Override
    public void fire(Connection conn, ResultSet oldRow, ResultSet newRow) throws SQLException {
        if (newRow == null) {
            return;
        }
        String numeroPedido = newRow.getString("NUMERO_PEDIDO");
        if (numeroPedido == null || numeroPedido.isBlank()) {
            newRow.updateString("NUMERO_PEDIDO", TestIdentifiers.nextNumeroPedido());
        }

        Object totalObj = newRow.getObject("TOTAL");
        Object valorTotalObj = newRow.getObject("VALOR_TOTAL");

        if (totalObj == null && valorTotalObj != null) {
            double val = ((Number) valorTotalObj).doubleValue();
            newRow.updateDouble("TOTAL", val);
        } else if (valorTotalObj == null && totalObj != null) {
            double val = ((Number) totalObj).doubleValue();
            newRow.updateBigDecimal("VALOR_TOTAL", BigDecimal.valueOf(val));
        } else if (totalObj == null) {
            newRow.updateDouble("TOTAL", 0.0);
            newRow.updateBigDecimal("VALOR_TOTAL", BigDecimal.ZERO);
        }
    }
}
