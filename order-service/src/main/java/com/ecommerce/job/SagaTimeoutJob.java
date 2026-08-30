package com.ecommerce.job;

import com.ecommerce.saga.OrderSagaOrchestrator;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import org.jboss.logging.Logger;

/**
 * Recovers order SAGAs whose current step never got an answer. A command can be lost, or land
 * while the service that owns the step is down, and nothing in the request path would ever
 * notice: the SAGA simply waits. This sweep is what turns that silence into either a re-sent
 * command or a decision to stop.
 *
 * <p>Each SAGA is recovered in its own transaction. A late reply arriving mid-sweep loses the
 * optimistic-lock race and fails that one SAGA, which must not take the rest of the batch with
 * it, so the failure is caught per order rather than around the loop.
 */
@ApplicationScoped
public class SagaTimeoutJob {

    private static final Logger LOG = Logger.getLogger(SagaTimeoutJob.class);

    private static final int BATCH_SIZE = 100;

    @Inject
    OrderSagaOrchestrator orchestrator;

    @Scheduled(every = "{order.saga.sweep-interval}", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    public void recoverStalledSagas() {
        List<Long> overdue = orchestrator.ordersWithOverdueReplies(BATCH_SIZE);
        if (overdue.isEmpty()) {
            LOG.debug("No order SAGA is waiting past its deadline");
            return;
        }

        LOG.infof("Recovering %d order SAGA(s) waiting past their deadline", overdue.size());
        for (Long orderId : overdue) {
            recoverQuietly(orderId);
        }
    }

    private void recoverQuietly(Long orderId) {
        try {
            orchestrator.recoverTimedOut(orderId);
        } catch (Exception e) {
            LOG.errorf(e, "Could not recover the SAGA of order %d; leaving it for the next sweep", orderId);
        }
    }
}
