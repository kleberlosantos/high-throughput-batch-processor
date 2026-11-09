package com.vetto.batch.io;

import com.vetto.batch.model.Policy;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Streams CSV rows without loading the whole file as a single string.
 * Expected header: id,product_code,age,face_amount,version
 */
public final class PolicyCsvLoader {

    public List<Policy> load(Path csvPath) throws IOException {
        List<Policy> policies = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(csvPath)) {
            String header = reader.readLine();
            if (header == null) {
                return policies;
            }
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                String[] parts = line.split(",", -1);
                if (parts.length < 5) {
                    throw new IOException("Malformed CSV row: " + line);
                }
                policies.add(new Policy(
                        Long.parseLong(parts[0].trim()),
                        parts[1].trim(),
                        Integer.parseInt(parts[2].trim()),
                        new BigDecimal(parts[3].trim()),
                        Integer.parseInt(parts[4].trim())
                ));
            }
        }
        return policies;
    }
}
