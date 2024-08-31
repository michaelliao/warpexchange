package com.itranswarp.exchange.model.trade;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import org.springframework.lang.Nullable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.itranswarp.exchange.enums.Direction;
import com.itranswarp.exchange.enums.OrderStatus;
import com.itranswarp.exchange.model.support.EntitySupport;

/**
 * Order entity.
 */
@Entity
@Table(name = "orders")
public class OrderEntity implements EntitySupport, Comparable<OrderEntity> {

    /**
     * Primary key: assigned order id.
     */
    @Id
    @Column(nullable = false, updatable = false)
    public Long id;

    /**
     * event id (a.k.a sequenceId) that create this order. ASC only.
     */
    @Column(nullable = false, updatable = false)
    public long sequenceId;

    /**
     * Order direction.
     */
    @Column(nullable = false, updatable = false, length = VAR_ENUM)
    public Direction direction;

    /**
     * User id of this order.
     */
    @Column(nullable = false, updatable = false)
    public Long userId;

    /**
     * Order status.
     */
    @Column(nullable = false, updatable = false, length = VAR_ENUM)
    public OrderStatus status;

    public void updateOrder(BigDecimal unfilledQuantity, OrderStatus status, long updatedAt) {
        this.version++;
        this.unfilledQuantity = unfilledQuantity;
        this.status = status;
        this.updatedAt = updatedAt;
        this.version++;
    }

    /**
     * The limit-order price. MUST NOT change after insert.
     */
    @Column(nullable = false, updatable = false, precision = PRECISION, scale = SCALE)
    public BigDecimal price;

    /**
     * Created time (milliseconds).
     */
    @Column(nullable = false, updatable = false)
    public long createdAt;

    /**
     * Updated time (milliseconds).
     */
    @Column(nullable = false, updatable = false)
    public long updatedAt;

    private int version;

    @Transient
    @JsonIgnore
    public int getVersion() {
        return this.version;
    }

    /**
     * The order quantity. MUST NOT change after insert.
     */
    @Column(nullable = false, updatable = false, precision = PRECISION, scale = SCALE)
    public BigDecimal quantity;

    /**
     * How much unfilled during match.
     */
    @Column(nullable = false, updatable = false, precision = PRECISION, scale = SCALE)
    public BigDecimal unfilledQuantity;

    @Nullable
    public OrderEntity copy() {
        OrderEntity entity = new OrderEntity();
        int ver = this.version;
        entity.status = this.status;
        entity.unfilledQuantity = this.unfilledQuantity;
        entity.updatedAt = this.updatedAt;
        if (ver != this.version) {
            return null;
        }

        entity.createdAt = this.createdAt;
        entity.direction = this.direction;
        entity.id = this.id;
        entity.price = this.price;
        entity.quantity = this.quantity;
        entity.sequenceId = this.sequenceId;
        entity.userId = this.userId;
        return entity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof OrderEntity) {
            OrderEntity e = (OrderEntity) o;
            return this.id.longValue() == e.id.longValue();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public String toString() {
        return "OrderEntity [id=" + id + ", sequenceId=" + sequenceId + ", direction=" + direction + ", userId="
                + userId + ", status=" + status + ", price=" + price + ", createdAt=" + createdAt + ", updatedAt="
                + updatedAt + ", version=" + version + ", quantity=" + quantity + ", unfilledQuantity="
                + unfilledQuantity + "]";
    }

    /**
     * 按OrderID排序
     */
    @Override
    public int compareTo(OrderEntity o) {
        return Long.compare(this.id.longValue(), o.id.longValue());
    }
}
