package com.vetto.batch.repository;

import com.vetto.batch.model.Policy;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class PolicyRepository {

    private final Database database;

    public PolicyRepository(Database database) {
        this.database = database;
    }

    public void seedSynthetic(int count) throws SQLException {
        String[] products = {"TERM10", "TERM20", "WHOLE", "UL"};
        try (Connection connection = database.open();
             PreparedStatement ps = connection.prepareStatement(
                     "INSERT INTO policy (id, product_code, age, face_amount, version) VALUES (?, ?, ?, ?, 0)")) {
            for (int i = 1; i <= count; i++) {
                ThreadLocalRandom rnd = ThreadLocalRandom.current();
                ps.setLong(1, i);
                ps.setString(2, products[rnd.nextInt(products.length)]);
                ps.setInt(3, rnd.nextInt(20, 81));
                ps.setBigDecimal(4, BigDecimal.valueOf(rnd.nextInt(50, 1001) * 1000L));
                ps.addBatch();
                if (i % 1000 == 0) {
                    ps.executeBatch();
                }
            }
            ps.executeBatch();
            connection.commit();
        }
    }

    public void upsertAll(List<Policy> policies) throws SQLException {
        try (Connection connection = database.open();
             PreparedStatement ps = connection.prepareStatement(
                     "MERGE INTO policy (id, product_code, age, face_amount, version) KEY(id) VALUES (?, ?, ?, ?, ?)")) {
            for (Policy policy : policies) {
                ps.setLong(1, policy.id());
                ps.setString(2, policy.productCode());
                ps.setInt(3, policy.age());
                ps.setBigDecimal(4, policy.faceAmount());
                ps.setInt(5, policy.version());
                ps.addBatch();
            }
            ps.executeBatch();
            connection.commit();
        }
    }

    public List<Policy> findPage(long afterId, int limit) throws SQLException {
        List<Policy> policies = new ArrayList<>(limit);
        try (Connection connection = database.open();
             PreparedStatement ps = connection.prepareStatement(
                     "SELECT id, product_code, age, face_amount, version FROM policy WHERE id > ? ORDER BY id FETCH FIRST ? ROWS ONLY")) {
            ps.setLong(1, afterId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    policies.add(new Policy(
                            rs.getLong(1),
                            rs.getString(2),
                            rs.getInt(3),
                            rs.getBigDecimal(4),
                            rs.getInt(5)
                    ));
                }
            }
            connection.commit();
        }
        return policies;
    }

    /**
     * Optimistic update: succeeds only when the row version still matches.
     * Returns {@code true} when the update applied.
     */
    public boolean bumpVersion(long policyId, int expectedVersion) throws SQLException {
        try (Connection connection = database.open();
             PreparedStatement ps = connection.prepareStatement(
                     "UPDATE policy SET version = version + 1 WHERE id = ? AND version = ?")) {
            ps.setLong(1, policyId);
            ps.setInt(2, expectedVersion);
            int updated = ps.executeUpdate();
            connection.commit();
            return updated == 1;
        }
    }

    public Policy findById(long policyId) throws SQLException {
        try (Connection connection = database.open();
             PreparedStatement ps = connection.prepareStatement(
                     "SELECT id, product_code, age, face_amount, version FROM policy WHERE id = ?")) {
            ps.setLong(1, policyId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                Policy policy = new Policy(
                        rs.getLong(1),
                        rs.getString(2),
                        rs.getInt(3),
                        rs.getBigDecimal(4),
                        rs.getInt(5)
                );
                connection.commit();
                return policy;
            }
        }
    }
}
