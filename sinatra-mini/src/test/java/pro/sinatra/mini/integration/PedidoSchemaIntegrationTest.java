package pro.sinatra.mini.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class PedidoSchemaIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Test
    public void h2ShouldContainCompositeIndexOnPedido() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            String tableName = "PEDIDO"; // H2 stores uppercase by default

            ResultSet rs = meta.getIndexInfo(null, null, tableName, false, false);

            // Map index name -> ordered list of columns
            Map<String, SortedMap<Integer, String>> indexes = new HashMap<>();

            while (rs.next()) {
                String idxName = rs.getString("INDEX_NAME");
                String colName = rs.getString("COLUMN_NAME");
                int ordinal = rs.getInt("ORDINAL_POSITION");
                if (idxName == null || colName == null) continue;
                indexes.computeIfAbsent(idxName, k -> new TreeMap<>()).put(ordinal, colName.toLowerCase());
            }

            // Find index that matches loja_id, data_pedido in that order
            boolean found = false;
            for (Map.Entry<String, SortedMap<Integer, String>> e : indexes.entrySet()) {
                List<String> cols = new ArrayList<>(e.getValue().values());
                if (cols.size() >= 2) {
                    String first = cols.get(0).toLowerCase();
                    String second = cols.get(1).toLowerCase();
                    if ((first.equals("loja_id") && second.equals("data_pedido")) ||
                        (first.equals("loja_id") && second.equals("data_pedido"))) {
                        found = true;
                        break;
                    }
                }
            }

            assertTrue(found, "Composite index on (loja_id, data_pedido) must exist in H2 schema");
        }
    }
}
