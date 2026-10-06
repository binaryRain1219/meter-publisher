package com.ms.meterpublisher.reading;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public class RawReadingRepository {

    private final JdbcTemplate jdbcTemplate;

    public RawReadingRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // 디바이스의 가장 최근 원본 값. 재시작 시 누적값/순번을 이어가기 위해 쓴다.
    public Optional<LastReading> findLatest(long deviceId) {
        return jdbcTemplate.query("""
                        SELECT cumulativeKwh, seq
                        FROM rawReading
                        WHERE deviceId = ?
                        ORDER BY measuredAt DESC
                        LIMIT 1
                        """,
                (rs, rowNum) -> new LastReading(rs.getBigDecimal("cumulativeKwh"), rs.getLong("seq")),
                deviceId
        ).stream().findFirst();
    }

    public record LastReading(BigDecimal cumulativeKwh, long seq) {
    }
}
