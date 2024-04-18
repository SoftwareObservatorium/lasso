package de.uni_mannheim.swt.lasso.data;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Data loading based on DuckDB
 *
 * @author Marcus Kessel
 *
 * @see <a href="https://duckdb.org/docs/api/java">DuckDB Java</a>
 */
public class DuckDbLoader {

    private static final Logger LOG = LoggerFactory
            .getLogger(DuckDbLoader.class);

    private JdbcTemplate jdbcTemplate;

    static {
        try {
            Class.forName("org.duckdb.DuckDBDriver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Load {@link JdbcTemplate}
     *
     * @return
     */
    public JdbcTemplate getJdbcTemplate() {
        synchronized (DuckDbLoader.class) {
            if(jdbcTemplate != null) {
                return jdbcTemplate;
            }

            jdbcTemplate = new JdbcTemplate();

            HikariConfig config = new HikariConfig();
            config.setDriverClassName("org.duckdb.DuckDBDriver");
            //config.setMaximumPoolSize(10);
            //config.setMaxLifetime(3);
            config.setJdbcUrl("jdbc:duckdb:");
            HikariDataSource ds = new HikariDataSource(config);
            jdbcTemplate.setDataSource(ds);

            return jdbcTemplate;
        }
    }
}
