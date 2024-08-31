package com.itranswarp.exchange.messaging;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerEndpoint;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.BatchMessageListener;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.support.TopicPartitionOffset;
import org.springframework.kafka.support.converter.MessageConverter;
import org.springframework.stereotype.Component;

import com.itranswarp.exchange.message.AbstractMessage;
import com.itranswarp.exchange.support.LoggerSupport;

import jakarta.annotation.PostConstruct;

/**
 * 接收和发送消息的入口
 */
@Component
public class MessagingFactory extends LoggerSupport {

    @Autowired
    private MessageTypes messageTypes;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ConcurrentKafkaListenerContainerFactory<String, String> listenerContainerFactory;

    @Autowired
    private KafkaAdmin kafkaAdmin;

    @PostConstruct
    public void init() throws InterruptedException, ExecutionException {
        logger.info("init kafka admin...");
        try (AdminClient client = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
            // 查询当前所有topic:
            Set<String> allTopics = client.listTopics().names().get();
            // 自动创建不存在的topic:
            List<NewTopic> newTopics = new ArrayList<>();
            for (Messaging.Topic topic : Messaging.Topic.values()) {
                if (!allTopics.contains(topic.name())) {
                    newTopics.add(new NewTopic(topic.name(), topic.getPartitions(), (short) 1));
                }
            }
            if (!newTopics.isEmpty()) {
                client.createTopics(newTopics);
                newTopics.forEach(t -> {
                    logger.warn("auto-create kafka topics when init MessagingFactory: {}", t);
                });
            }
        }
        logger.info("init MessagingFactory ok.");
    }

    public <T extends AbstractMessage> MessageProducer<T> createMessageProducer(Messaging.Topic topic,
            Class<T> messageClass) {
        logger.info("try create message producer for topic {}...", topic);
        final String name = topic.name();
        return new MessageProducer<>() {
            @Override
            public void sendMessage(AbstractMessage message) {
                kafkaTemplate.send(name, messageTypes.serialize(message));
            }
        };
    }

    public <T extends AbstractMessage> MessageConsumer createBatchMessageListener(Messaging.Topic topic, String groupId,
            BatchMessageHandler<T> messageHandler) {
        return createBatchMessageListener(topic, groupId, messageHandler, null);
    }

    public <T extends AbstractMessage> MessageConsumer createBatchMessageListener(Messaging.Topic topic, String groupId,
            BatchMessageHandler<T> messageHandler, CommonErrorHandler errorHandler) {
        logger.info("try create batch message listener for topic {}: group id = {}...", topic, groupId);
        ConcurrentMessageListenerContainer<String, String> listenerContainer = listenerContainerFactory
                .createListenerContainer(new KafkaListenerEndpointAdapter() {
                    @Override
                    public String getGroupId() {
                        return groupId;
                    }

                    @Override
                    public Collection<String> getTopics() {
                        return List.of(topic.name());
                    }
                });
        listenerContainer.setupMessageListener(new BatchMessageListener<String, String>() {
            @Override
            @SuppressWarnings("unchecked")
            public void onMessage(List<ConsumerRecord<String, String>> data) {
                List<T> messages = new ArrayList<>(data.size());
                for (ConsumerRecord<String, String> record : data) {
                    AbstractMessage message = messageTypes.deserialize(record.value());
                    messages.add((T) message);
                }
                messageHandler.processMessages(messages);
            }
        });
        if (errorHandler != null) {
            listenerContainer.setCommonErrorHandler(errorHandler);
        }
        listenerContainer.start();
        return listenerContainer::stop;
    }
}

class KafkaListenerEndpointAdapter implements KafkaListenerEndpoint {

    @Override
    public String getId() {
        return null;
    }

    @Override
    public String getGroupId() {
        return null;
    }

    @Override
    public String getGroup() {
        return null;
    }

    @Override
    public Collection<String> getTopics() {
        return List.of();
    }

    @Override
    public Pattern getTopicPattern() {
        return null;
    }

    @Override
    public String getClientIdPrefix() {
        return null;
    }

    @Override
    public Integer getConcurrency() {
        return Integer.valueOf(1);
    }

    @Override
    public Boolean getAutoStartup() {
        return Boolean.FALSE;
    }

    @Override
    public void setupListenerContainer(MessageListenerContainer listenerContainer, MessageConverter messageConverter) {
    }

    @Override
    public TopicPartitionOffset[] getTopicPartitionsToAssign() {
        return null;
    }

    @Override
    public boolean isSplitIterables() {
        return false;
    }
}
