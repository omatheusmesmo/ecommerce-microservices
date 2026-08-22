package com.ecommerce.job;

import io.agroal.api.AgroalDataSource;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.jboss.logging.Logger;

/**
 * The JDBC idempotency store enforces expiry lazily on read, so rows whose key is
 * never retried stay behind forever. This bounds the table by deleting entries past
 * both their lock and response TTL.
 */
@ApplicationScoped
public class IdempotencyEntryCleanup {

    private static final Logger LOG = Logger.getLogger(IdempotencyEntryCleanup.class);

    private static final String DELETE_EXPIRED_SQL =
            "DELETE FROM idempotency_entry WHERE (in_flight = 1 AND lock_expires_at < ?)"
                    + " OR (in_flight = 0 AND response_expires_at IS NOT NULL AND response_expires_at < ?)";

    @Inject
    AgroalDataSource dataSource;

    @Scheduled(every = "1h", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    public void deleteExpiredEntries() {
        long now = System.currentTimeMillis();
        try (Connection connection = dataSource.getConnection();
                PreparedStatement delete = connection.prepareStatement(DELETE_EXPIRED_SQL)) {
            delete.setLong(1, now);
            delete.setLong(2, now);
            int deleted = delete.executeUpdate();
            if (deleted > 0) {
                LOG.infof("Deleted %d expired idempotency entry/entries", deleted);
            }
        } catch (SQLException e) {
            LOG.error("Could not delete expired idempotency entries", e);
        }
    }
}
