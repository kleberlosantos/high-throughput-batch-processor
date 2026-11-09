package com.vetto.batch.repository;

import com.vetto.batch.model.ReserveResult;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public final class ResultRepository {

    private final Database database;

    public ResultRepository(Database database) {
        this.database = database;
    }

    public void bulkUpsert(List<ReserveResult> results) throws SQLException {
        if (results.isEmpty()) {
            return;
        }
        try (Connection connection = database.open();
             PreparedStatement ps = connection.prepareStatement(
                     """
                     MERGE INTO reserve_result (policy_id, reserve_amount, rate_key)
                     KEY(policy_id) VALUES (?, ?, ?)
                     """)) {
            for (ReserveResult result : results) {
                ps.setLong(1, result.policyId());
                ps.setBigDecimal(2, result.reserveAmount());
                ps.setString(3, result.rateKey());
                ps.addBatch();
            }
            ps.executeBatch();
            connection.commit();
        }
    }
}
