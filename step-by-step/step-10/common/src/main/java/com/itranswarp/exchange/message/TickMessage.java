package com.itranswarp.exchange.message;

import java.util.List;

import com.itranswarp.exchange.model.quotation.TickEntity;

public class TickMessage extends AbstractMessage {

    public long sequenceId;

    public List<TickEntity> ticks;

}
