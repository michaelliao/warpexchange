package org.puppylab.exchange.messaging;

import java.util.List;

import org.puppylab.exchange.message.AbstractMessage;

@FunctionalInterface
public interface BatchMessageHandler<T extends AbstractMessage> {

    void processMessages(List<T> messages);

}
