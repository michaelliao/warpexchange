package com.itranswarp.exchange.model.trade;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.itranswarp.exchange.model.support.EntitySupport;

@Entity
@Table(name = "unique_events")
public class UniqueEventEntity implements EntitySupport {

    @Id
    @Column(nullable = false, updatable = false, length = VAR_CHAR_50)
    public String uniqueId;

    /**
     * Which event associated.
     */
    @Column(nullable = false, updatable = false)
    public long sequenceId;

    /**
     * Created time (milliseconds). Set after sequenced.
     */
    @Column(nullable = false, updatable = false)
    public long createdAt;

    @Override
    public String toString() {
        return "UniqueEventEntity [uniqueId=" + uniqueId + ", sequenceId=" + sequenceId + ", createdAt=" + createdAt
                + "]";
    }
}
