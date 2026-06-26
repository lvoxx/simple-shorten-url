package io.lvoxx.ssurl.dashboard.dto.response;

import java.time.LocalDate;

/** One bucket of a clicks-over-time series (daily granularity). */
public record TimeSeriesPoint(LocalDate date, long clicks) {
}
