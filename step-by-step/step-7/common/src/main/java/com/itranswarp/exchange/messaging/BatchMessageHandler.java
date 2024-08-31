package com.itranswarp.exchange.messaging;

import java.util.List;

import com.itranswarp.exchange.message.AbstractMessage;

@FunctionalInterface
public interface BatchMessageHandler<T extends AbstractMessage> {

    void processMessages(List<T> messages);

}
