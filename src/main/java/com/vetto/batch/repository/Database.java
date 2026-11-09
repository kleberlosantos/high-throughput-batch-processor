package com.vetto.batch.repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

public final class Database implements AutoCloseable {

    private final String jdbcUrl;

    public Database(String jdbcUrl) {
        this.jdbcUrl = Objects.requireNonNull(jdbcUrl, "jdbcUrl");
    }

    public Connection open() throws SQLException {
        Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "");
        connection.setAutoCommit(false);
        return connection;
    }

    public void migrate() throws SQLException {
        try (Connection connection = open(); Statement st = connection.createStatement()) {
            st.execute("""
                    CREATE TABLE IF NOT EXISTS actuarial_rate (
                        product_code VARCHAR(32) NOT NULL,
                        age_band     INT NOT NULL,
                        factor       DECIMAL(12, 6) NOT NULL,
                        PRIMARY KEY (product_code, age_band)
                    )
                    """);
            st.execute("""
                    CREATE TABLE IF NOT EXISTS policy (
                        id           BIGINT PRIMARY KEY,
                        product_code VARCHAR(32) NOT NULL,
                        age          INT NOT NULL,
                        face_amount  DECIMAL(18, 2) NOT NULL,
                        version      INT NOT NULL DEFAULT 0
                    )
                    """);
            st.execute("""
                    CREATE TABLE IF NOT EXISTS reserve_result (
                        policy_id      BIGINT PRIMARY KEY,
                        reserve_amount DECIMAL(18, 2) NOT NULL,
                        rate_key       VARCHAR(64) NOT NULL,
                        computed_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                    """);
            connection.commit();
        }
    }

    @Override
    public void close() {
        // H2 in-memory; nothing to shut down explicitly for the demo.
    }
}
