package com.itranswarp.exchange.model.trade;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import com.itranswarp.exchange.enums.Direction;
import com.itranswarp.exchange.enums.OrderStatus;
import com.itranswarp.exchange.enums.ClearingType;
import com.itranswarp.exchange.model.support.EntitySupport;

@Entity
@Table(name = "clearings", uniqueConstraints = @UniqueConstraint(name = "UNI_SEQ_ORD_CORD", columnNames = "sequenceId,orderId,counterOrderId"))
public class ClearingEntity implements EntitySupport {

    public static final String IDX_USER_ID = "IDX_USER_ID";

    /**
     * Primary key: auto-increment id.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, updatable = false)
    public Long id;

    /**
     * Sequence id.
     */
    @Column(nullable = false, updatable = false)
    public long sequenceId;

    /**
     * The order id of this clearing record.
     */
    @Column(nullable = false, updatable = false, length = VAR_ENUM)
    public Long orderId;

    /**
     * The counter order id of this clearing record.
     */
    @Column(nullable = false, updatable = false, length = VAR_ENUM)
    public Long counterOrderId;

    /**
     * User id.
     */
    @Column(nullable = false, updatable = false)
    public Long userId;

    /**
     * Counter user id.
     */
    @Column(nullable = false, updatable = false)
    public Long counterUserId;

    /**
     * Direction of order.
     */
    @Column(nullable = false, updatable = false, length = VAR_ENUM)
    public Direction direction;

    /**
     * Clearing type.
     */
    @Column(nullable = false, updatable = false, length = VAR_ENUM)
    public ClearingType type;

    /**
     * The match price for this clearing.
     */
    @Column(nullable = false, updatable = false, precision = PRECISION, scale = SCALE)
    public BigDecimal matchPrice;

    /**
     * The match quantity.
     */
    @Column(nullable = false, updatable = false, precision = PRECISION, scale = SCALE)
    public BigDecimal matchQuantity;

    /**
     * Order status after clearing.
     */
    @Column(nullable = false, updatable = false, length = VAR_ENUM)
    public OrderStatus orderStatusAfterClearing;

    /**
     * Unfilled quantity after clearing.
     */
    @Column(nullable = false, updatable = false, precision = PRECISION, scale = SCALE)
    public BigDecimal orderUnfilledQuantityAfterClearing;

    /**
     * Created time (milliseconds).
     */
    @Column(nullable = false, updatable = false)
    public long createdAt;

    @Override
    public String toString() {
        return "ClearingEntity [id=" + id + ", sequenceId=" + sequenceId + ", orderId=" + orderId + ", counterOrderId="
                + counterOrderId + ", userId=" + userId + ", counterUserId=" + counterUserId + ", direction="
                + direction + ", type=" + type + ", matchPrice=" + matchPrice + ", matchQuantity=" + matchQuantity
                + ", orderStatusAfterClearing=" + orderStatusAfterClearing + ", orderUnfilledQuantityAfterClearing="
                + orderUnfilledQuantityAfterClearing + ", createdAt=" + createdAt + "]";
    }
}
