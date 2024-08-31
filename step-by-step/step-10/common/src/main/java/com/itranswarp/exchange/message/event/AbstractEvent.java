package com.itranswarp.exchange.message.event;

import org.springframework.lang.Nullable;

import com.itranswarp.exchange.message.AbstractMessage;

public class AbstractEvent extends AbstractMessage {

    /**
     * Message id, set after sequenced.
     */
    public long sequenceId;

    /**
     * Previous message sequence id.
     */
    public long previousId;

    /**
     * Unique ID or null if not set.
     */
    @Nullable
    public String uniqueId;
}
