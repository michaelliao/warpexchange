package org.puppylab.exchange.store;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import org.puppylab.exchange.db.DbTemplate;
import org.puppylab.exchange.message.event.AbstractEvent;
import org.puppylab.exchange.messaging.MessageTypes;
import org.puppylab.exchange.model.support.EntitySupport;
import org.puppylab.exchange.model.trade.EventEntity;
import org.puppylab.exchange.support.LoggerSupport;

@Component
@Transactional
public class StoreService extends LoggerSupport {

    @Autowired
    MessageTypes messageTypes;

    @Autowired
    DbTemplate dbTemplate;

    public List<AbstractEvent> loadEventsFromDb(long lastEventId) {
        List<EventEntity> events = this.dbTemplate.from(EventEntity.class).where("sequenceId > ?", lastEventId)
                .orderBy("sequenceId").limit(100000).list();
        return events.stream().map(event -> (AbstractEvent) messageTypes.deserialize(event.data))
                .collect(Collectors.toList());
    }

    public void insertIgnore(List<? extends EntitySupport> list) {
        dbTemplate.insertIgnore(list);
    }
}
