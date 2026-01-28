package com.ecommerce.service;

import com.ecommerce.repository.RefreshTokenRepository;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;

@ApplicationScoped
public class RefreshTokenCleanupService {

    private static final Logger LOG = Logger.getLogger(RefreshTokenCleanupService.class);

    @Inject
    RefreshTokenRepository refreshTokenRepository;

    @Scheduled(every = "P1D")
    @Transactional
    public void cleanupExpiredTokens() {
        LOG.debug("Starting cleanup of expired refresh tokens");
        Long deleted = refreshTokenRepository.deleteExpired();
        LOG.infof("Cleaned up %d expired/revoked refresh tokens", deleted);
    }
}