package com.itranswarp.exchange.sequencer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.itranswarp.exchange.message.event.AbstractEvent;
import com.itranswarp.exchange.messaging.MessageTypes;
import com.itranswarp.exchange.model.trade.EventEntity;
import com.itranswarp.exchange.model.trade.UniqueEventEntity;
import com.itranswarp.exchange.support.AbstractDbService;

/**
 * Process events as batch.
 */
@Component
@Transactional(rollbackFor = Throwable.class)
public class SequenceHandler extends AbstractDbService {

    private long lastTimestamp = 0;

    /**
     * Set sequence for each message, persist into database as batch.
     * 
     * @return Sequenced messages.
     */
    public List<AbstractEvent> sequenceMessages(final MessageTypes messageTypes, final AtomicLong sequence,
            final List<AbstractEvent> messages) throws Exception {
        final long t = System.currentTimeMillis();
        if (t < this.lastTimestamp) {
            logger.warn("[Sequence] current time {} is turned back from {}!", t, this.lastTimestamp);
        } else {
            this.lastTimestamp = t;
        }
        List<UniqueEventEntity> uniques = null;
        Set<String> uniqueKeys = null;
        List<AbstractEvent> sequencedMessages = new ArrayList<>(messages.size());
        List<EventEntity> events = new ArrayList<>(messages.size());
        for (AbstractEvent message : messages) {
            UniqueEventEntity unique = null;
            final String uniqueId = message.uniqueId;
            // check uniqueId:
            if (uniqueId != null) {
                if ((uniqueKeys != null && uniqueKeys.contains(uniqueId))
                        || db.fetch(UniqueEventEntity.class, uniqueId) != null) {
                    logger.warn("ignore processed unique message: {}", message);
                    continue;
                }
                unique = new UniqueEventEntity();
                unique.uniqueId = uniqueId;
                unique.createdAt = message.createdAt;
                if (uniques == null) {
                    uniques = new ArrayList<>();
                }
                uniques.add(unique);
                if (uniqueKeys == null) {
                    uniqueKeys = new HashSet<>();
                }
                uniqueKeys.add(uniqueId);
                logger.info("unique event {} sequenced.", uniqueId);
            }

            final long previousId = sequence.get();
            final long currentId = sequence.incrementAndGet();

            // 先设置message的sequenceId / previouseId / createdAt，再序列化并落库:
            message.sequenceId = currentId;
            message.previousId = previousId;
            message.createdAt = this.lastTimestamp;

            // 如果此消息关联了UniqueEvent，给UniqueEvent加上相同的sequenceId：
            if (unique != null) {
                unique.sequenceId = message.sequenceId;
            }

            // create AbstractEvent and save to db later:
            EventEntity event = new EventEntity();
            event.previousId = previousId;
            event.sequenceId = currentId;

            event.data = messageTypes.serialize(message);
            event.createdAt = this.lastTimestamp; // same as message.createdAt
            events.add(event);

            // will send later:
            sequencedMessages.add(message);
        }

        if (uniques != null) {
            db.insert(uniques);
        }
        db.insert(events);
        return sequencedMessages;
    }

    public long getMaxSequenceId() {
        EventEntity last = db.from(EventEntity.class).orderBy("sequenceId").desc().first();
        if (last == null) {
            logger.info("no max sequenceId found. set max sequenceId = 0.");
            return 0;
        }
        this.lastTimestamp = last.createdAt;
        logger.info("find max sequenceId = {}, last timestamp = {}", last.sequenceId, this.lastTimestamp);
        return last.sequenceId;
    }
}
