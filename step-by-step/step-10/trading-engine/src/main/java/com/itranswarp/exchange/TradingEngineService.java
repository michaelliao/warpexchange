package com.itranswarp.exchange;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.itranswarp.exchange.assets.AssetService;
import com.itranswarp.exchange.assets.Asset;
import com.itranswarp.exchange.assets.Transfer;
import com.itranswarp.exchange.bean.OrderBookBean;
import com.itranswarp.exchange.clearing.ClearingService;
import com.itranswarp.exchange.enums.AssetEnum;
import com.itranswarp.exchange.enums.Direction;
import com.itranswarp.exchange.enums.MatchType;
import com.itranswarp.exchange.enums.UserType;
import com.itranswarp.exchange.match.MatchDetailRecord;
import com.itranswarp.exchange.match.MatchEngine;
import com.itranswarp.exchange.match.MatchResult;
import com.itranswarp.exchange.message.ApiResultMessage;
import com.itranswarp.exchange.message.NotificationMessage;
import com.itranswarp.exchange.message.TickMessage;
import com.itranswarp.exchange.message.event.AbstractEvent;
import com.itranswarp.exchange.message.event.OrderCancelEvent;
import com.itranswarp.exchange.message.event.OrderRequestEvent;
import com.itranswarp.exchange.message.event.TransferEvent;
import com.itranswarp.exchange.messaging.MessageConsumer;
import com.itranswarp.exchange.messaging.MessageProducer;
import com.itranswarp.exchange.messaging.Messaging;
import com.itranswarp.exchange.messaging.Messaging.Topic;
import com.itranswarp.exchange.messaging.MessagingFactory;
import com.itranswarp.exchange.model.quotation.TickEntity;
import com.itranswarp.exchange.model.trade.MatchDetailEntity;
import com.itranswarp.exchange.model.trade.OrderEntity;
import com.itranswarp.exchange.order.OrderService;
import com.itranswarp.exchange.redis.RedisCache;
import com.itranswarp.exchange.redis.RedisService;
import com.itranswarp.exchange.store.StoreService;
import com.itranswarp.exchange.support.LoggerSupport;
import com.itranswarp.exchange.util.IpUtil;
import com.itranswarp.exchange.util.JsonUtil;

@Component
public class TradingEngineService extends LoggerSupport {

    @Autowired(required = false)
    ZoneId zoneId = ZoneId.systemDefault();

    @Value("#{exchangeConfiguration.orderBookDepth}")
    int orderBookDepth = 100;

    @Value("#{exchangeConfiguration.debugMode}")
    boolean debugMode = false;

    boolean fatalError = false;

    @Autowired
    AssetService assetService;

    @Autowired
    OrderService orderService;

    @Autowired
    MatchEngine matchEngine;

    @Autowired
    ClearingService clearingService;

    @Autowired
    MessagingFactory messagingFactory;

    @Autowired
    StoreService storeService;

    @Autowired
    RedisService redisService;

    private MessageConsumer consumer;

    private MessageProducer<TickMessage> producer;

    private long lastSequenceId = 0;

    private boolean orderBookChanged = false;

    private String shaUpdateOrderBookLua;

    private Thread tickThread;
    private Thread notifyThread;
    private Thread apiResultThread;
    private Thread orderBookThread;
    private Thread dbThread;

    private OrderBookBean latestOrderBook = null;
    private Queue<List<OrderEntity>> orderQueue = new ConcurrentLinkedQueue<>();
    private Queue<List<MatchDetailEntity>> matchQueue = new ConcurrentLinkedQueue<>();
    private Queue<TickMessage> tickQueue = new ConcurrentLinkedQueue<>();
    private Queue<ApiResultMessage> apiResultQueue = new ConcurrentLinkedQueue<>();
    private Queue<NotificationMessage> notificationQueue = new ConcurrentLinkedQueue<>();

    @PostConstruct
    public void init() {
        this.shaUpdateOrderBookLua = this.redisService.loadScriptFromClassPath("/redis/update-orderbook.lua");
        this.consumer = this.messagingFactory.createBatchMessageListener(Messaging.Topic.TRADE, IpUtil.getHostId(),
                this::processMessages);
        this.producer = this.messagingFactory.createMessageProducer(Topic.TICK, TickMessage.class);
        this.tickThread = new Thread(this::runTickThread, "async-tick");
        this.tickThread.start();
        this.notifyThread = new Thread(this::runNotifyThread, "async-notify");
        this.notifyThread.start();
        this.orderBookThread = new Thread(this::runOrderBookThread, "async-orderbook");
        this.orderBookThread.start();
        this.apiResultThread = new Thread(this::runApiResultThread, "async-api-result");
        this.apiResultThread.start();
        this.dbThread = new Thread(this::runDbThread, "async-db");
        this.dbThread.start();
    }

    @PreDestroy
    public void destroy() {
        this.consumer.stop();
        this.orderBookThread.interrupt();
        this.dbThread.interrupt();
    }

    private void runTickThread() {
        logger.info("start tick thread...");
        for (;;) {
            List<TickMessage> msgs = new ArrayList<>();
            for (;;) {
                TickMessage msg = tickQueue.poll();
                if (msg != null) {
                    msgs.add(msg);
                    if (msgs.size() >= 1000) {
                        break;
                    }
                } else {
                    break;
                }
            }
            if (!msgs.isEmpty()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("send {} tick messages...", msgs.size());
                }
                this.producer.sendMessages(msgs);
            } else {
                // 无TickMessage时，暂停1ms:
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    logger.warn("{} was interrupted.", Thread.currentThread().getName());
                    break;
                }
            }
        }
    }

    private void runNotifyThread() {
        logger.info("start publish notify to redis...");
        for (;;) {
            NotificationMessage msg = this.notificationQueue.poll();
            if (msg != null) {
                redisService.publish(RedisCache.Topic.NOTIFICATION, JsonUtil.writeJson(msg));
            } else {
                // 无推送时，暂停1ms:
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    logger.warn("{} was interrupted.", Thread.currentThread().getName());
                    break;
                }
            }
        }
    }

    private void runApiResultThread() {
        logger.info("start publish api result to redis...");
        for (;;) {
            ApiResultMessage result = this.apiResultQueue.poll();
            if (result != null) {
                redisService.publish(RedisCache.Topic.TRADING_API_RESULT, JsonUtil.writeJson(result));
            } else {
                // 无推送时，暂停1ms:
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    logger.warn("{} was interrupted.", Thread.currentThread().getName());
                    break;
                }
            }
        }
    }

    private void runOrderBookThread() {
        logger.info("start update orderbook snapshot to redis...");
        long lastSequenceId = 0;
        for (;;) {
            // 获取OrderBookBean的引用，确保后续操作针对局部变量而非成员变量:
            final OrderBookBean orderBook = this.latestOrderBook;
            // 仅在OrderBookBean更新后刷新Redis:
            if (orderBook != null && orderBook.sequenceId > lastSequenceId) {
                if (logger.isDebugEnabled()) {
                    logger.debug("update orderbook snapshot at sequence id {}...", orderBook.sequenceId);
                }
                redisService.executeScriptReturnBoolean(this.shaUpdateOrderBookLua,
                        // keys: [cache-key]
                        new String[] { RedisCache.Key.ORDER_BOOK },
                        // args: [sequenceId, json-data]
                        new String[] { String.valueOf(orderBook.sequenceId), JsonUtil.writeJson(orderBook) });
                lastSequenceId = orderBook.sequenceId;
            } else {
                // 无更新时，暂停1ms:
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    logger.warn("{} was interrupted.", Thread.currentThread().getName());
                    break;
                }
            }
        }
    }

    private void runDbThread() {
        logger.info("start batch insert to db...");
        for (;;) {
            try {
                saveToDb();
            } catch (InterruptedException e) {
                logger.warn("{} was interrupted.", Thread.currentThread().getName());
                break;
            }
        }
    }

    // called by dbExecutor thread only:
    private void saveToDb() throws InterruptedException {
        if (!matchQueue.isEmpty()) {
            List<MatchDetailEntity> batch = new ArrayList<>(1000);
            for (;;) {
                List<MatchDetailEntity> matches = matchQueue.poll();
                if (matches != null) {
                    batch.addAll(matches);
                    if (batch.size() >= 1000) {
                        break;
                    }
                } else {
                    break;
                }
            }
            batch.sort(MatchDetailEntity::compareTo);
            if (logger.isDebugEnabled()) {
                logger.debug("batch insert {} match details...", batch.size());
            }
            this.storeService.insertIgnore(batch);
        }
        if (!orderQueue.isEmpty()) {
            List<OrderEntity> batch = new ArrayList<>(1000);
            for (;;) {
                List<OrderEntity> orders = orderQueue.poll();
                if (orders != null) {
                    batch.addAll(orders);
                    if (batch.size() >= 1000) {
                        break;
                    }
                } else {
                    break;
                }
            }
            batch.sort(OrderEntity::compareTo);
            if (logger.isDebugEnabled()) {
                logger.debug("batch insert {} orders...", batch.size());
            }
            this.storeService.insertIgnore(batch);
        }
        if (matchQueue.isEmpty()) {
            Thread.sleep(1);
        }
    }

    public void processMessages(List<AbstractEvent> messages) {
        this.orderBookChanged = false;
        for (AbstractEvent message : messages) {
            processEvent(message);
        }
        if (this.orderBookChanged) {
            // 获取最新的OrderBook快照:
            this.latestOrderBook = this.matchEngine.getOrderBook(this.orderBookDepth);
        }
    }

    public void processEvent(AbstractEvent event) {
        if (this.fatalError) {
            return;
        }
        if (event.sequenceId <= this.lastSequenceId) {
            logger.warn("skip duplicate event: {}", event);
            return;
        }
        if (event.previousId > this.lastSequenceId) {
            logger.warn("event lost: expected previous id {} but actual {} for event {}", this.lastSequenceId,
                    event.previousId, event);
            List<AbstractEvent> events = this.storeService.loadEventsFromDb(this.lastSequenceId);
            if (events.isEmpty()) {
                logger.error("cannot load lost event from db.");
                panic();
                return;
            }
            for (AbstractEvent e : events) {
                this.processEvent(e);
            }
            return;
        }
        if (event.previousId != lastSequenceId) {
            logger.error("bad event: expected previous id {} but actual {} for event: {}", this.lastSequenceId,
                    event.previousId, event);
            panic();
            return;
        }
        if (logger.isDebugEnabled()) {
            logger.debug("process event {} -> {}: {}...", this.lastSequenceId, event.sequenceId, event);
        }
        try {
            if (event instanceof OrderRequestEvent) {
                createOrder((OrderRequestEvent) event);
            } else if (event instanceof OrderCancelEvent) {
                cancelOrder((OrderCancelEvent) event);
            } else if (event instanceof TransferEvent) {
                transfer((TransferEvent) event);
            } else {
                logger.error("unable to process event type: {}", event.getClass().getName());
                panic();
                return;
            }
        } catch (Exception e) {
            logger.error("process event error.", e);
            panic();
            return;
        }
        this.lastSequenceId = event.sequenceId;
        if (logger.isDebugEnabled()) {
            logger.debug("set last processed sequence id: {}...", this.lastSequenceId);
        }
        if (debugMode) {
            this.validate();
            this.debug();
        }
    }

    private void panic() {
        logger.error("application panic. exit now...");
        this.fatalError = true;
        System.exit(1);
    }

    boolean transfer(TransferEvent event) {
        boolean ok = this.assetService.tryTransfer(Transfer.AVAILABLE_TO_AVAILABLE, event.fromUserId, event.toUserId,
                event.asset, event.amount, event.sufficient);
        return ok;
    }

    void createOrder(OrderRequestEvent event) {
        ZonedDateTime zdt = Instant.ofEpochMilli(event.createdAt).atZone(zoneId);
        int year = zdt.getYear();
        int month = zdt.getMonth().getValue();
        long orderId = event.sequenceId * 10000 + (year * 100 + month);
        OrderEntity order = this.orderService.createOrder(event.sequenceId, event.createdAt, orderId, event.userId,
                event.direction, event.price, event.quantity);
        if (order == null) {
            logger.warn("create order failed.");
            // 推送失败结果:
            this.apiResultQueue.add(ApiResultMessage.createOrderFailed(event.refId, event.createdAt));
            return;
        }
        MatchResult result = this.matchEngine.processOrder(event.sequenceId, order);
        this.clearingService.clearMatchResult(result);
        // 推送成功结果,注意必须复制一份OrderEntity,因为将异步序列化:
        this.apiResultQueue.add(ApiResultMessage.orderSuccess(event.refId, order.copy(), event.createdAt));
        this.orderBookChanged = true;
        // 收集Notification:
        List<NotificationMessage> notifications = new ArrayList<>();
        notifications.add(createNotification(event.createdAt, "order_matched", order.userId, order.copy()));
        // 收集已完成的OrderEntity并生成MatchDetailEntity, TickEntity:
        if (!result.matchDetails.isEmpty()) {
            List<OrderEntity> closedOrders = new ArrayList<>();
            List<MatchDetailEntity> matchDetails = new ArrayList<>();
            List<TickEntity> ticks = new ArrayList<>();
            if (result.takerOrder.status.isFinalStatus) {
                closedOrders.add(result.takerOrder);
            }
            for (MatchDetailRecord detail : result.matchDetails) {
                OrderEntity maker = detail.makerOrder();
                notifications.add(createNotification(event.createdAt, "order_matched", maker.userId, maker.copy()));
                if (maker.status.isFinalStatus) {
                    closedOrders.add(maker);
                }
                MatchDetailEntity takerDetail = generateMatchDetailEntity(event.sequenceId, event.createdAt, detail,
                        true);
                MatchDetailEntity makerDetail = generateMatchDetailEntity(event.sequenceId, event.createdAt, detail,
                        false);
                matchDetails.add(takerDetail);
                matchDetails.add(makerDetail);
                TickEntity tick = new TickEntity();
                tick.sequenceId = event.sequenceId;
                tick.takerOrderId = detail.takerOrder().id;
                tick.makerOrderId = detail.makerOrder().id;
                tick.price = detail.price();
                tick.quantity = detail.quantity();
                tick.takerDirection = detail.takerOrder().direction == Direction.BUY;
                tick.createdAt = event.createdAt;
                ticks.add(tick);
            }
            // 异步写入数据库:
            this.orderQueue.add(closedOrders);
            this.matchQueue.add(matchDetails);
            // 异步发送Tick消息:
            TickMessage msg = new TickMessage();
            msg.sequenceId = event.sequenceId;
            msg.createdAt = event.createdAt;
            msg.ticks = ticks;
            this.tickQueue.add(msg);
            // 异步通知OrderMatch:
            this.notificationQueue.addAll(notifications);
        }
    }

    private NotificationMessage createNotification(long ts, String type, Long userId, Object data) {
        NotificationMessage msg = new NotificationMessage();
        msg.createdAt = ts;
        msg.type = type;
        msg.userId = userId;
        msg.data = data;
        return msg;
    }

    MatchDetailEntity generateMatchDetailEntity(long sequenceId, long timestamp, MatchDetailRecord detail,
            boolean forTaker) {
        MatchDetailEntity d = new MatchDetailEntity();
        d.sequenceId = sequenceId;
        d.orderId = forTaker ? detail.takerOrder().id : detail.makerOrder().id;
        d.counterOrderId = forTaker ? detail.makerOrder().id : detail.takerOrder().id;
        d.direction = forTaker ? detail.takerOrder().direction : detail.makerOrder().direction;
        d.price = detail.price();
        d.quantity = detail.quantity();
        d.type = forTaker ? MatchType.TAKER : MatchType.MAKER;
        d.userId = forTaker ? detail.takerOrder().userId : detail.makerOrder().userId;
        d.counterUserId = forTaker ? detail.makerOrder().userId : detail.takerOrder().userId;
        d.createdAt = timestamp;
        return d;
    }

    void cancelOrder(OrderCancelEvent event) {
        OrderEntity order = this.orderService.getOrder(event.refOrderId);
        // 未找到活动订单或订单不属于该用户:
        if (order == null || order.userId.longValue() != event.userId.longValue()) {
            // 发送失败消息:
            this.apiResultQueue.add(ApiResultMessage.cancelOrderFailed(event.refId, event.createdAt));
            return;
        }
        this.matchEngine.cancel(event.createdAt, order);
        this.clearingService.clearCancelOrder(order);
        this.orderBookChanged = true;
        // 发送成功消息:
        this.apiResultQueue.add(ApiResultMessage.orderSuccess(event.refId, order, event.createdAt));
        this.notificationQueue.add(createNotification(event.createdAt, "order_canceled", order.userId, order));
    }

    public void debug() {
        System.out.println("========== trading engine ==========");
        this.assetService.debug();
        this.orderService.debug();
        this.matchEngine.debug();
        System.out.println("========== // trading engine ==========");
    }

    void validate() {
        logger.debug("start validate...");
        validateAssets();
        validateOrders();
        validateMatchEngine();
        logger.debug("validate ok.");
    }

    void validateAssets() {
        // 验证系统资产完整性:
        BigDecimal totalUSD = BigDecimal.ZERO;
        BigDecimal totalBTC = BigDecimal.ZERO;
        for (Entry<Long, ConcurrentMap<AssetEnum, Asset>> userEntry : this.assetService.getUserAssets().entrySet()) {
            Long userId = userEntry.getKey();
            ConcurrentMap<AssetEnum, Asset> assets = userEntry.getValue();
            for (Entry<AssetEnum, Asset> entry : assets.entrySet()) {
                AssetEnum assetId = entry.getKey();
                Asset asset = entry.getValue();
                if (userId.longValue() == UserType.DEBT.getInternalUserId()) {
                    // 系统负债账户available不允许为正:
                    require(asset.getAvailable().signum() <= 0, "Debt has positive available: " + asset);
                    // 系统负债账户frozen必须为0:
                    require(asset.getFrozen().signum() == 0, "Debt has non-zero frozen: " + asset);
                } else {
                    // 交易用户的available/frozen不允许为负数:
                    require(asset.getAvailable().signum() >= 0, "Trader has negative available: " + asset);
                    require(asset.getFrozen().signum() >= 0, "Trader has negative frozen: " + asset);
                }
                switch (assetId) {
                case USD -> {
                    totalUSD = totalUSD.add(asset.getTotal());
                }
                case BTC -> {
                    totalBTC = totalBTC.add(asset.getTotal());
                }
                default -> require(false, "Unexpected asset id: " + assetId);
                }
            }
        }
        // 各类别资产总额为0:
        require(totalUSD.signum() == 0, "Non zero USD balance: " + totalUSD);
        require(totalBTC.signum() == 0, "Non zero BTC balance: " + totalBTC);
    }

    void validateOrders() {
        // 验证订单:
        Map<Long, Map<AssetEnum, BigDecimal>> userOrderFrozen = new HashMap<>();
        for (Entry<Long, OrderEntity> entry : this.orderService.getActiveOrders().entrySet()) {
            OrderEntity order = entry.getValue();
            require(order.unfilledQuantity.signum() > 0, "Active order must have positive unfilled amount: " + order);
            switch (order.direction) {
            case BUY -> {
                // 订单必须在MatchEngine中:
                require(this.matchEngine.buyBook.exist(order), "order not found in buy book: " + order);
                // 累计冻结的USD:
                userOrderFrozen.putIfAbsent(order.userId, new HashMap<>());
                Map<AssetEnum, BigDecimal> frozenAssets = userOrderFrozen.get(order.userId);
                frozenAssets.putIfAbsent(AssetEnum.USD, BigDecimal.ZERO);
                BigDecimal frozen = frozenAssets.get(AssetEnum.USD);
                frozenAssets.put(AssetEnum.USD, frozen.add(order.price.multiply(order.unfilledQuantity)));
            }
            case SELL -> {
                // 订单必须在MatchEngine中:
                require(this.matchEngine.sellBook.exist(order), "order not found in sell book: " + order);
                // 累计冻结的BTC:
                userOrderFrozen.putIfAbsent(order.userId, new HashMap<>());
                Map<AssetEnum, BigDecimal> frozenAssets = userOrderFrozen.get(order.userId);
                frozenAssets.putIfAbsent(AssetEnum.BTC, BigDecimal.ZERO);
                BigDecimal frozen = frozenAssets.get(AssetEnum.BTC);
                frozenAssets.put(AssetEnum.BTC, frozen.add(order.unfilledQuantity));
            }
            default -> require(false, "Unexpected order direction: " + order.direction);
            }
        }
        // 订单冻结的累计金额必须和Asset冻结一致:
        for (Entry<Long, ConcurrentMap<AssetEnum, Asset>> userEntry : this.assetService.getUserAssets().entrySet()) {
            Long userId = userEntry.getKey();
            ConcurrentMap<AssetEnum, Asset> assets = userEntry.getValue();
            for (Entry<AssetEnum, Asset> entry : assets.entrySet()) {
                AssetEnum assetId = entry.getKey();
                Asset asset = entry.getValue();
                if (asset.getFrozen().signum() > 0) {
                    Map<AssetEnum, BigDecimal> orderFrozen = userOrderFrozen.get(userId);
                    require(orderFrozen != null, "No order frozen found for user: " + userId + ", asset: " + asset);
                    BigDecimal frozen = orderFrozen.get(assetId);
                    require(frozen != null, "No order frozen found for asset: " + asset);
                    require(frozen.compareTo(asset.getFrozen()) == 0,
                            "Order frozen " + frozen + " is not equals to asset frozen: " + asset);
                    // 从userOrderFrozen中删除已验证的Asset数据:
                    orderFrozen.remove(assetId);
                }
            }
        }
        // userOrderFrozen不存在未验证的Asset数据:
        for (Entry<Long, Map<AssetEnum, BigDecimal>> userEntry : userOrderFrozen.entrySet()) {
            Long userId = userEntry.getKey();
            Map<AssetEnum, BigDecimal> frozenAssets = userEntry.getValue();
            require(frozenAssets.isEmpty(), "User " + userId + " has unexpected frozen for order: " + frozenAssets);
        }
    }

    void validateMatchEngine() {
        // OrderBook的Order必须在ActiveOrders中:
        Map<Long, OrderEntity> copyOfActiveOrders = new HashMap<>(this.orderService.getActiveOrders());
        for (OrderEntity order : this.matchEngine.buyBook.book.values()) {
            require(copyOfActiveOrders.remove(order.id) == order,
                    "Order in buy book is not in active orders: " + order);
        }
        for (OrderEntity order : this.matchEngine.sellBook.book.values()) {
            require(copyOfActiveOrders.remove(order.id) == order,
                    "Order in sell book is not in active orders: " + order);
        }
        // activeOrders的所有Order必须在Order Book中:
        require(copyOfActiveOrders.isEmpty(), "Not all active orders are in order book.");
    }

    void require(boolean condition, String errorMessage) {
        if (!condition) {
            logger.error("validate failed: {}", errorMessage);
            panic();
        }
    }
}
