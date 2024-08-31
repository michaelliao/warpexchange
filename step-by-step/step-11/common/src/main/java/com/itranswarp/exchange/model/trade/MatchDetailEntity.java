package com.itranswarp.exchange.model.trade;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import com.itranswarp.exchange.enums.Direction;
import com.itranswarp.exchange.enums.MatchType;
import com.itranswarp.exchange.model.support.EntitySupport;

/**
 * Store the read-only match detail for each order.
 */
@Entity
@Table(name = "match_details", uniqueConstraints = @UniqueConstraint(name = "UNI_OID_COID", columnNames = { "orderId",
        "counterOrderId" }), indexes = @Index(name = "IDX_OID_CT", columnList = "orderId,createdAt"))
public class MatchDetailEntity implements EntitySupport, Comparable<MatchDetailEntity> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, updatable = false)
    public long id;

    /**
     * SequenceId of the event which triggered this match detail.
     */
    @Column(nullable = false, updatable = false)
    public long sequenceId;

    @Column(nullable = false, updatable = false)
    public Long orderId;

    @Column(nullable = false, updatable = false)
    public Long counterOrderId;

    /**
     * User id.
     */
    @Column(nullable = false, updatable = false)
    public Long userId;

    /**
     * Counter order's user id.
     */
    @Column(nullable = false, updatable = false)
    public Long counterUserId;

    @Column(nullable = false, updatable = false, length = VAR_ENUM)
    public MatchType type;

    /**
     * The match direction.
     */
    @Column(nullable = false, updatable = false, length = VAR_ENUM)
    public Direction direction;

    /**
     * The match price.
     */
    @Column(nullable = false, updatable = false, precision = PRECISION, scale = SCALE)
    public BigDecimal price;

    /**
     * The match quantity.
     */
    @Column(nullable = false, updatable = false, precision = PRECISION, scale = SCALE)
    public BigDecimal quantity;

    /**
     * Match detail created time (milliseconds).
     */
    @Column(nullable = false, updatable = false)
    public long createdAt;

    /**
     * 按OrderId, CounterOrderId排序
     */
    @Override
    public int compareTo(MatchDetailEntity o) {
        int cmp = Long.compare(this.orderId.longValue(), o.orderId.longValue());
        if (cmp == 0) {
            cmp = Long.compare(this.counterOrderId.longValue(), o.orderId.longValue());
        }
        return cmp;
    }
}
