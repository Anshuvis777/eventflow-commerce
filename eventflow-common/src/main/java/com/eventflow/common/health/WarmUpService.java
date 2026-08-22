package com.eventflow.common.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

/**
 * Keeps Neon Postgres and the Render instance warm.
 *
 * Neon serverless suspends the DB after ~5 min idle; the first query after wake
 * takes 2-5s. Render Free sleeps after 15 min idle (cold start 30-60s).
 * This scheduler pings the DB every 4 minutes so both stay warm.
 *
 * Enable per service by setting keepalive.enabled=true (or KEEPALIVE_ENABLED=true env).
 * Disabled by default so local dev / tests are not affected.
 */
@Slf4j
@Component
public class WarmUpService {

    private final DataSource dataSource;

    @Value("${keepalive.enabled:false}")
    private boolean enabled;

    public WarmUpService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Scheduled(fixedDelayString = "${keepalive.interval-ms:240000}")
    public void pingDatabase() {
        if (!enabled) return;
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("SELECT 1");
            log.debug("Keepalive ping OK");
        } catch (Exception e) {
            log.warn("Keepalive ping failed: {}", e.getMessage());
        }
    }
}
