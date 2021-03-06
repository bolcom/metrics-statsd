/**
 * Copyright (C) 2014 metrics-statsd contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.bol.dropwizard.metrics.reporting;

import com.bol.dropwizard.metrics.reporting.statsd.StatsD;
import com.codahale.metrics.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

/**
 * A reporter which publishes metric values to a StatsD server.
 *
 * @see <a href="https://github.com/etsy/statsd">StatsD</a>
 */
@NotThreadSafe
public class StatsDReporter extends ScheduledReporter {
    private static final Logger LOG = LoggerFactory.getLogger(StatsDReporter.class);

    private final StatsD statsD;
    private final String prefix;
    private final String[] tags;
    private final boolean skipUnchangedTimerDurationMetrics;
    private final boolean skipUnchangedHistogramMetrics;
    private final Map<String, Long> metricCounters = new HashMap<String, Long>();

    private StatsDReporter(
       MetricRegistry registry,
       StatsD statsD,
       String prefix,
       String[] tags,
       TimeUnit rateUnit,
       TimeUnit durationUnit,
       MetricFilter filter,
       boolean skipUnchangedTimerDurationMetrics,
       boolean skipUnchangedHistogramMetrics
    ) {
        super(registry, "statsd-reporter", filter, rateUnit, durationUnit);
        this.statsD = statsD;
        this.skipUnchangedTimerDurationMetrics = skipUnchangedTimerDurationMetrics;
        this.skipUnchangedHistogramMetrics = skipUnchangedHistogramMetrics;
        this.prefix = prefix;
        this.tags = tags;
    }

    /**
     * Returns a new {@link Builder} for {@link StatsDReporter}.
     *
     * @param registry
     *            the registry to report
     * @return a {@link Builder} instance for a {@link StatsDReporter}
     */
    public static Builder forRegistry(MetricRegistry registry) {
        return new Builder(registry);
    }

    /**
     * A builder for {@link StatsDReporter} instances. Defaults to not using a
     * prefix, no tags, converting rates to events/second, converting durations to
     * milliseconds, and not filtering metrics.
     */
    @NotThreadSafe
    public static final class Builder {
        private final MetricRegistry registry;
        private String prefix;
        private boolean skipUnchangedTimerDurationMetrics;
        private boolean skipUnchangedHistogramMetrics;
        private String[] tags;
        private TimeUnit rateUnit;
        private TimeUnit durationUnit;
        private MetricFilter filter;

        private Builder(MetricRegistry registry) {
            this.registry = registry;
            this.prefix = null;
            this.tags = null;
            this.skipUnchangedTimerDurationMetrics = true;
            this.skipUnchangedHistogramMetrics = true;
            this.rateUnit = TimeUnit.SECONDS;
            this.durationUnit = TimeUnit.MILLISECONDS;
            this.filter = MetricFilter.ALL;
        }

        /**
         * Prefix all metric names with the given string.
         *
         * @param prefix
         *            the prefix for all metric names
         * @return {@code this}
         */
        public Builder prefixedWith(@Nullable String prefix) {
            this.prefix = prefix;
            return this;
        }

        /**
         * Add all given tags to all metrics
         * @param tags the tags for all metrics
         * @return {@code this}
         */
        public Builder withTags(@Nullable String... tags) {
            this.tags = tags;
            return this;
        }

        /**
         * Convert rates to the given time unit.
         *
         * @param rateUnit
         *            a unit of time
         * @return {@code this}
         */
        public Builder convertRatesTo(TimeUnit rateUnit) {
            this.rateUnit = rateUnit;
            return this;
        }

        /**
         * Convert durations to the given time unit.
         *
         * @param durationUnit
         *            a unit of time
         * @return {@code this}
         */
        public Builder convertDurationsTo(TimeUnit durationUnit) {
            this.durationUnit = durationUnit;
            return this;
        }

        /**
         * Only report metrics which match the given filter.
         *
         * @param filter
         *            a {@link MetricFilter}
         * @return {@code this}
         */
        public Builder filter(MetricFilter filter) {
            this.filter = filter;
            return this;
        }

        /**
         * If unchanged timer durations and histogram metrics are to be skipped from being reported to StatsD.
         * <p>
         * This will prevent Metrics from showing an old value in the StatsD backend (often Graphite) when nothing actually
         * changed.
         * <p>
         * Counts and rates are always reported. You need counts and rates to always be reported because else you can't do the statistical calculations
         * on them within the backend.
         * <p>
         * This reporter figures out which metrics should be skipped by keeping track of the count of this metrics.
         * If the count didn't change, and they will increase if a new value is recorded, then it won't send the metrics to StatsD.
         * <p>
         * This is actually a shortcut for calling the {@link #skipUnchangedHistogramMetrics} and {@link #skipUnchangedTimerDurationMetrics} with
         * the same value.
         *
         * @param skipUnchangedMetrics If unchanged metrics are to be skipped from being reported to StatsD
         * @return {@code this}
         */
        public Builder skipUnchangedMetrics(boolean skipUnchangedMetrics) {
            this.skipUnchangedTimerDurationMetrics = skipUnchangedMetrics;
            this.skipUnchangedHistogramMetrics = skipUnchangedMetrics;
            return this;
        }

        /**
         * If unchanged timer duration metrics are to be skipped from being reported to StatsD.
         * <p>
         * This will prevent from showing an old value in the StatsD backend (often Graphite) when nothing actually changed.
         * <p>
         * Counts and rates are always reported. You need counts and rates to always be reported because else you can't do the statistical calculations
         * on them within the backend.
         * <p>
         * This reporter figures out which metrics should be skipped by keeping track of the count of the timer.
         * If the count didn't change, and they will increase if a new value is recorded, then it won't send the metrics to StatsD.
         * <p>
         * Default: true
         *
         * @param skipUnchangedTimerDurationMetrics If unchanged timer metrics should not be reported to StatsD
         * @return {@code this}
         */
        public Builder skipUnchangedTimerDurationMetrics(boolean skipUnchangedTimerDurationMetrics) {
            this.skipUnchangedTimerDurationMetrics = skipUnchangedTimerDurationMetrics;
            return this;
        }

        /**
         * If unchanged histogram metrics are to be skipped from being reported to StatsD.
         * <p>
         * This will prevent from showing an old value in the StatsD backend (often Graphite) when nothing actually changed.
         * <p>
         * The count is always reported. You need the counts to always be reported because else you can't do the statistical calculations
         * on it within the backend.
         * <p>
         * This reporter figures out which metrics should be skipped by keeping track of the count of the histogram.
         * If the count didn't change, and they will increase if a new value is recorded, then it won't send the metrics to StatsD.
         * <p>
         * Default: true
         *
         * @param skipUnchangedHistogramMetrics If unchanged histogram metrics should always be reported to StatsD
         * @return {@code this}
         */
        public Builder skipUnchangedHistogramMetrics(boolean skipUnchangedHistogramMetrics) {
            this.skipUnchangedHistogramMetrics = skipUnchangedHistogramMetrics;
            return this;
        }

        /**
         * Builds a {@link StatsDReporter} with the given properties, sending
         * metrics to StatsD at the given host and port.
         *
         * @param host
         *            the hostname of the StatsD server.
         * @param port
         *            the port of the StatsD server. This is typically 8125.
         * @return a {@link StatsDReporter}
         */
        public StatsDReporter build(String host, int port) {
            return build(new StatsD(host, port));
        }

        /**
         * Builds a {@link StatsDReporter} with the given properties, sending
         * metrics using the given {@link StatsD} client.
         *
         * @param statsD
         *            a {@link StatsD} client
         * @return a {@link StatsDReporter}
         */
        public StatsDReporter build(StatsD statsD) {
            return new StatsDReporter(registry, statsD, prefix, tags, rateUnit, durationUnit, filter, skipUnchangedTimerDurationMetrics, skipUnchangedHistogramMetrics);
        }
    }

    @Override
    @SuppressWarnings("rawtypes")
    // Metrics 3.0 interface specifies the raw Gauge type
    public void report(
        SortedMap<String, Gauge> gauges,
        SortedMap<String, Counter> counters,
        SortedMap<String, Histogram> histograms,
        SortedMap<String, Meter> meters,
        SortedMap<String, Timer> timers
    ) {

        try {
            statsD.connect();

            for (Map.Entry<String, Gauge> entry : gauges.entrySet()) {
                reportGauge(entry.getKey(), entry.getValue());
            }

            for (Map.Entry<String, Counter> entry : counters.entrySet()) {
                reportCounter(entry.getKey(), entry.getValue());
            }

            for (Map.Entry<String, Histogram> entry : histograms.entrySet()) {
                reportHistogram(entry.getKey(), entry.getValue());
            }

            for (Map.Entry<String, Meter> entry : meters.entrySet()) {
                reportMetered(entry.getKey(), entry.getValue());
            }

            for (Map.Entry<String, Timer> entry : timers.entrySet()) {
                reportTimer(entry.getKey(), entry.getValue());
            }
        } catch (IOException e) {
            LOG.warn("Unable to report to StatsD", statsD, e);
        } finally {
            try {
                statsD.close();
            } catch (IOException e) {
                LOG.debug("Error disconnecting from StatsD", statsD, e);
            }
        }
    }

    private boolean metricChanged(String metricName, long currentCount) {
        Long previousCount = metricCounters.get(metricName);
        metricCounters.put(metricName, currentCount);

        return previousCount == null || previousCount != currentCount;
    }

    private void reportTimer(String name, Timer timer) {
        final Snapshot snapshot = timer.getSnapshot();

        if(!skipUnchangedTimerDurationMetrics || metricChanged(name, timer.getCount())) {
            statsD.send(prefix(name, "max"), formatNumber(convertDuration(snapshot.getMax())), tags);
            statsD.send(prefix(name, "mean"), formatNumber(convertDuration(snapshot.getMean())), tags);
            statsD.send(prefix(name, "min"), formatNumber(convertDuration(snapshot.getMin())), tags);
            statsD.send(prefix(name, "stddev"), formatNumber(convertDuration(snapshot.getStdDev())), tags);
            statsD.send(prefix(name, "p50"), formatNumber(convertDuration(snapshot.getMedian())), tags);
            statsD.send(prefix(name, "p75"), formatNumber(convertDuration(snapshot.get75thPercentile())), tags);
            statsD.send(prefix(name, "p95"), formatNumber(convertDuration(snapshot.get95thPercentile())), tags);
            statsD.send(prefix(name, "p98"), formatNumber(convertDuration(snapshot.get98thPercentile())), tags);
            statsD.send(prefix(name, "p99"), formatNumber(convertDuration(snapshot.get99thPercentile())), tags);
            statsD.send(prefix(name, "p999"), formatNumber(convertDuration(snapshot.get999thPercentile())), tags);
        }
        reportMeteredChecked(name, timer);
    }

    private void reportMetered(String name, Metered meter) {
        reportMeteredChecked(name, meter);
    }

    private void reportMeteredChecked(String name, Metered meter) {
        statsD.send(prefix(name, "count"), formatNumber(meter.getCount()), tags);
        statsD.send(prefix(name, "m1_rate"), formatNumber(convertRate(meter.getOneMinuteRate())), tags);
        statsD.send(prefix(name, "m5_rate"), formatNumber(convertRate(meter.getFiveMinuteRate())), tags);
        statsD.send(prefix(name, "m15_rate"), formatNumber(convertRate(meter.getFifteenMinuteRate())), tags);
        statsD.send(prefix(name, "mean_rate"), formatNumber(convertRate(meter.getMeanRate())), tags);
    }

    private void reportHistogram(String name, Histogram histogram) {
        final Snapshot snapshot = histogram.getSnapshot();
        long count = histogram.getCount();

        statsD.send(prefix(name, "count"), formatNumber(count), tags);

        if(!skipUnchangedHistogramMetrics || metricChanged(name, count)) {
            statsD.send(prefix(name, "max"), formatNumber(snapshot.getMax()), tags);
            statsD.send(prefix(name, "mean"), formatNumber(snapshot.getMean()), tags);
            statsD.send(prefix(name, "min"), formatNumber(snapshot.getMin()), tags);
            statsD.send(prefix(name, "stddev"), formatNumber(snapshot.getStdDev()), tags);
            statsD.send(prefix(name, "p50"), formatNumber(snapshot.getMedian()), tags);
            statsD.send(prefix(name, "p75"), formatNumber(snapshot.get75thPercentile()), tags);
            statsD.send(prefix(name, "p95"), formatNumber(snapshot.get95thPercentile()), tags);
            statsD.send(prefix(name, "p98"), formatNumber(snapshot.get98thPercentile()), tags);
            statsD.send(prefix(name, "p99"), formatNumber(snapshot.get99thPercentile()), tags);
            statsD.send(prefix(name, "p999"), formatNumber(snapshot.get999thPercentile()), tags);
        }
    }

    private void reportCounter(String name, Counter counter) {
        statsD.send(prefix(name), formatNumber(counter.getCount()), tags);
    }

    @SuppressWarnings("rawtypes")
    // Metrics 3.0 passes us the raw Gauge type
    private void reportGauge(String name, Gauge gauge) {
        final String value = format(gauge.getValue());
        if (value != null) {
            statsD.send(prefix(name), value, tags);
        }
    }

    @Nullable
    private String format(Object o) {
        if (o instanceof Float) {
            return formatNumber(((Float) o).doubleValue());
        } else if (o instanceof Double) {
            return formatNumber((Double) o);
        } else if (o instanceof Byte) {
            return formatNumber(((Byte) o).longValue());
        } else if (o instanceof Short) {
            return formatNumber(((Short) o).longValue());
        } else if (o instanceof Integer) {
            return formatNumber(((Integer) o).longValue());
        } else if (o instanceof Long) {
            return formatNumber((Long) o);
        } else if (o instanceof BigInteger) {
            return formatNumber((BigInteger) o);
        } else if (o instanceof BigDecimal) {
            return formatNumber(((BigDecimal) o).doubleValue());
        } else if (o instanceof Boolean) {
            return (Boolean) o ? "1" : "0";
        }
        return null;
    }

    private String prefix(String... components) {
        return MetricRegistry.name(prefix, components);
    }

    private String formatNumber(BigInteger n) {
        return String.valueOf(n);
    }

    private String formatNumber(long n) {
        return Long.toString(n);
    }

    private String formatNumber(double v) {
        return String.format(Locale.US, "%2.2f", v);
    }
}