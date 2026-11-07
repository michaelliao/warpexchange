package org.puppylab.exchange.message;

import java.util.List;

import org.puppylab.exchange.model.quotation.TickEntity;

public class TickMessage extends AbstractMessage {

    public long sequenceId;

    public List<TickEntity> ticks;

}
