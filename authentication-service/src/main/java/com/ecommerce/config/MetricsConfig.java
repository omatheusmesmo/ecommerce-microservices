package com.ecommerce.config;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class MetricsConfig {

    /**
     * Publishes full histogram buckets for HTTP server timers instead of client-side
     * percentiles, so p50/p95/p99 can be computed correctly across multiple replicas
     * (histogram_quantile in Prometheus) instead of a per-instance approximation that
     * can't be meaningfully averaged.
     */
    @Produces
    public MeterFilter enableHttpServerHistogramBuckets() {
        return new MeterFilter() {
            @Override
            public DistributionStatisticConfig configure(Meter.Id id, DistributionStatisticConfig config) {
                if (id.getName().startsWith("http.server.requests")) {
                    return DistributionStatisticConfig.builder()
                            .percentilesHistogram(true)
                            .build()
                            .merge(config);
                }
                return config;
            }
        };
    }
}
