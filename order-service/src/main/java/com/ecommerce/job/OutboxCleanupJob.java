package com.ecommerce.job;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import org.jboss.logging.Logger;

/**
 * Scheduled cleanup of the outbox table. Debezium reads outbox rows via CDC and
 * does not delete them on its own, so without this job the table grows without bound.
 */
@ApplicationScoped
public class OutboxCleanupJob {

    private static final Logger LOG = Logger.getLogger(OutboxCleanupJob.class);
    private static final int RETENTION_DAYS = 7;

    @Inject
    EntityManager entityManager;

    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupOldEvents() {
        int deletedCount = deleteEventsOlderThan(RETENTION_DAYS);
        if (deletedCount > 0) {
            LOG.infof("Deleted %d outbox events older than %d days", deletedCount, RETENTION_DAYS);
        } else {
            LOG.debug("No outbox events old enough to clean up");
        }
    }

    int deleteEventsOlderThan(int retentionDays) {
        LocalDateTime threshold = LocalDateTime.now().minusDays(retentionDays);
        return entityManager
                .createQuery("DELETE FROM OutboxEvent o WHERE o.createdAt < :threshold")
                .setParameter("threshold", threshold)
                .executeUpdate();
    }
}
