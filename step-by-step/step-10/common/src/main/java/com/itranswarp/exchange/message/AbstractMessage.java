package com.itranswarp.exchange.message;

import java.io.Serializable;

/**
 * Base message object for extends.
 */
public class AbstractMessage implements Serializable {

    /**
     * Reference id, or null if not set.
     */
    public String refId = null;

    /**
     * Message created at.
     */
    public long createdAt;

}
