package de.uni_mannheim.swt.lasso.sheets.service.srh;

import org.junit.jupiter.api.Test;

import java.sql.SQLException;

/**
 *
 * @author Marcus Kessel
 */
public class InMemorySRHTest {

    @Test
    public void test() throws SQLException {
        InMemorySRH duck = new InMemorySRH();
        duck.initialize();
    }
}
