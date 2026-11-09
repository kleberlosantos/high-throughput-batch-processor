package com.vetto.batch.repository;

import com.vetto.batch.model.ActuarialRate;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class ActuarialRateRepository {

    private final Database database;

    public ActuarialRateRepository(Database database) {
        this.database = database;
    }

    public void seedDefaults() throws SQLException {
        String[] products = {"TERM10", "TERM20", "WHOLE", "UL"};
        try (Connection connection = database.open();
             PreparedStatement ps = connection.prepareStatement(
                     "MERGE INTO actuarial_rate (product_code, age_band, factor) KEY(product_code, age_band) VALUES (?, ?, ?)")) {
            for (String product : products) {
                for (int ageBand = 10; ageBand <= 90; ageBand += 10) {
                    BigDecimal factor = BigDecimal.valueOf(0.01 + (ageBand / 1000.0) + product.hashCode() % 7 * 0.001)
                            .abs()
                            .setScale(6, java.math.RoundingMode.HALF_UP);
                    ps.setString(1, product);
                    ps.setInt(2, ageBand);
                    ps.setBigDecimal(3, factor);
                    ps.addBatch();
                }
            }
            ps.executeBatch();
            connection.commit();
        }
    }

    public ActuarialRate findRequired(String productCode, int ageBand) {
        try (Connection connection = database.open();
             PreparedStatement ps = connection.prepareStatement(
                     "SELECT product_code, age_band, factor FROM actuarial_rate WHERE product_code = ? AND age_band = ?")) {
            // Simulate a slightly expensive lookup (static table scan / hierarchical join stand-in).
            Thread.sleep(5);
            ps.setString(1, productCode);
            ps.setInt(2, ageBand);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalStateException("Missing actuarial rate for " + productCode + ":" + ageBand);
                }
                return new ActuarialRate(rs.getString(1), rs.getInt(2), rs.getBigDecimal(3));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while loading actuarial rate", e);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to load actuarial rate " + productCode + ":" + ageBand, e);
        }
    }

    public List<ActuarialRate> findAll() throws SQLException {
        List<ActuarialRate> rates = new ArrayList<>();
        try (Connection connection = database.open();
             PreparedStatement ps = connection.prepareStatement(
                     "SELECT product_code, age_band, factor FROM actuarial_rate");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rates.add(new ActuarialRate(rs.getString(1), rs.getInt(2), rs.getBigDecimal(3)));
            }
            connection.commit();
        }
        return rates;
    }
}
