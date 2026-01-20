package pro.sinatra.mini.entity;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import jakarta.persistence.Table;
import jakarta.persistence.Index;

public class PedidoEntityAnnotationTest {

    @Test
    public void tableAnnotationContainsCompositeIndex() {
        Class<Pedido> cls = Pedido.class;
        Table table = cls.getAnnotation(Table.class);
        assertNotNull(table, "@Table must be present on Pedido");

        Index[] indexes = table.indexes();
        assertNotNull(indexes, "indexes() should not be null");
        boolean found = false;
        for (Index idx : indexes) {
            if ("idx_pedido_loja_data".equalsIgnoreCase(idx.name())) {
                found = true;
                String cols = idx.columnList();
                assertNotNull(cols, "columnList must be defined for the index");
                String normalized = cols.replaceAll("\s+", "").toLowerCase();
                assertEquals("loja_id,data_pedido", normalized,
                        "Index columnList should be 'loja_id, data_pedido' (physical column names)");
            }
        }
        assertTrue(found, "Index 'idx_pedido_loja_data' must be declared on Pedido @Table");
    }
}
