package com.itranswarp.exchange.model.support;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;

/**
 * Store bars of second, minute, hour and day.
 */
@MappedSuperclass
public class AbstractBarEntity implements EntitySupport {

    /**
     * start timestamp in millisecond (included).
     */
    @Id
    @Column(nullable = false, updatable = false)
    public long startTime;

    @Column(nullable = false, updatable = false, precision = PRECISION, scale = SCALE)
    public BigDecimal openPrice;

    @Column(nullable = false, updatable = false, precision = PRECISION, scale = SCALE)
    public BigDecimal highPrice;

    @Column(nullable = false, updatable = false, precision = PRECISION, scale = SCALE)
    public BigDecimal lowPrice;

    @Column(nullable = false, updatable = false, precision = PRECISION, scale = SCALE)
    public BigDecimal closePrice;

    @Column(nullable = false, updatable = false, precision = PRECISION, scale = SCALE)
    public BigDecimal quantity;

    /**
     * Get compact bar data as array: [startTime, O, H, L, C, V].
     * 
     * @return Number[] array contains 6 elements.
     */
    @Transient
    public Number[] getBarData() {
        return new Number[] { this.startTime, this.openPrice, this.highPrice, this.lowPrice, this.closePrice,
                this.quantity };
    }

    public String toString(ZoneId zoneId) {
        ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(this.startTime), zoneId);
        String time = FORMATTERS.get(getClass().getSimpleName()).format(zdt);
        return String.format("{%s: startTime=%s, O=%s, H=%s, L=%s, C=%s, qty=%s}", this.getClass().getSimpleName(),
                time, this.openPrice, this.highPrice, this.lowPrice, this.closePrice, this.quantity);
    }

    @Override
    public String toString() {
        return toString(ZoneId.systemDefault());
    }

    static final Map<String, DateTimeFormatter> FORMATTERS = Map.of( //
            "SecBarEntity", DateTimeFormatter.ofPattern("HH:mm:ss", Locale.US), //
            "MinBarEntity", DateTimeFormatter.ofPattern("dd HH:mm", Locale.US), //
            "HourBarEntity", DateTimeFormatter.ofPattern("MM-dd HH", Locale.US), //
            "DayBarEntity", DateTimeFormatter.ofPattern("yy-MM-dd", Locale.US));
}
