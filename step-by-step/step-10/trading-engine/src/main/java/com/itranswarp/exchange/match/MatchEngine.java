package com.itranswarp.exchange.match;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.itranswarp.exchange.bean.OrderBookBean;
import com.itranswarp.exchange.enums.Direction;
import com.itranswarp.exchange.enums.OrderStatus;
import com.itranswarp.exchange.model.trade.OrderEntity;

@Component
public class MatchEngine {

    public final OrderBook buyBook = new OrderBook(Direction.BUY);
    public final OrderBook sellBook = new OrderBook(Direction.SELL);
    public BigDecimal marketPrice = BigDecimal.ZERO; // 最新市场价
    private long sequenceId;

    public MatchResult processOrder(long sequenceId, OrderEntity order) {
        return switch (order.direction) {
        case BUY -> processOrder(sequenceId, order, this.sellBook, this.buyBook);
        case SELL -> processOrder(sequenceId, order, this.buyBook, this.sellBook);
        default -> throw new IllegalArgumentException("Invalid direction.");
        };
    }

    /**
     * @param takerOrder  输入订单
     * @param makerBook   尝试匹配成交的OrderBook
     * @param anotherBook 未能完全成交后挂单的OrderBook
     * @return 成交结果
     */
    private MatchResult processOrder(long sequenceId, OrderEntity takerOrder, OrderBook makerBook,
            OrderBook anotherBook) {
        this.sequenceId = sequenceId;
        long ts = takerOrder.createdAt;
        MatchResult matchResult = new MatchResult(takerOrder);
        BigDecimal takerUnfilledQuantity = takerOrder.quantity;
        for (;;) {
            OrderEntity makerOrder = makerBook.getFirst();
            if (makerOrder == null) {
                // 对手盘不存在:
                break;
            }
            if (takerOrder.direction == Direction.BUY && takerOrder.price.compareTo(makerOrder.price) < 0) {
                // 买入订单价格比卖盘第一档价格低:
                break;
            } else if (takerOrder.direction == Direction.SELL && takerOrder.price.compareTo(makerOrder.price) > 0) {
                // 卖出订单价格比买盘第一档价格高:
                break;
            }
            // 以Maker价格成交:
            this.marketPrice = makerOrder.price;
            // 待成交数量为两者较小值:
            BigDecimal matchedQuantity = takerUnfilledQuantity.min(makerOrder.unfilledQuantity);
            // 成交记录:
            matchResult.add(makerOrder.price, matchedQuantity, makerOrder);
            // 更新成交后的订单数量:
            takerUnfilledQuantity = takerUnfilledQuantity.subtract(matchedQuantity);
            BigDecimal makerUnfilledQuantity = makerOrder.unfilledQuantity.subtract(matchedQuantity);
            // 对手盘完全成交后，从订单簿中删除:
            if (makerUnfilledQuantity.signum() == 0) {
                makerOrder.updateOrder(makerUnfilledQuantity, OrderStatus.FULLY_FILLED, ts);
                makerBook.remove(makerOrder);
            } else {
                // 对手盘部分成交:
                makerOrder.updateOrder(makerUnfilledQuantity, OrderStatus.PARTIAL_FILLED, ts);
            }
            // Taker订单完全成交后，退出循环:
            if (takerUnfilledQuantity.signum() == 0) {
                takerOrder.updateOrder(takerUnfilledQuantity, OrderStatus.FULLY_FILLED, ts);
                break;
            }
        }
        // Taker订单未完全成交时，放入订单簿:
        if (takerUnfilledQuantity.signum() > 0) {
            takerOrder.updateOrder(takerUnfilledQuantity,
                    takerUnfilledQuantity.compareTo(takerOrder.quantity) == 0 ? OrderStatus.PENDING
                            : OrderStatus.PARTIAL_FILLED,
                    ts);
            anotherBook.add(takerOrder);
        }
        return matchResult;
    }

    public void cancel(long ts, OrderEntity order) {
        OrderBook book = order.direction == Direction.BUY ? this.buyBook : this.sellBook;
        if (!book.remove(order)) {
            throw new IllegalArgumentException("Order not found in order book.");
        }
        OrderStatus status = order.unfilledQuantity.compareTo(order.quantity) == 0 ? OrderStatus.FULLY_CANCELLED
                : OrderStatus.PARTIAL_CANCELLED;
        order.updateOrder(order.unfilledQuantity, status, ts);
    }

    public OrderBookBean getOrderBook(int maxDepth) {
        return new OrderBookBean(this.sequenceId, this.marketPrice, this.buyBook.getOrderBook(maxDepth),
                this.sellBook.getOrderBook(maxDepth));
    }

    public void debug() {
        System.out.println("---------- match engine ----------");
        System.out.println(this.sellBook);
        System.out.println("  ----------");
        System.out.println("  " + this.marketPrice);
        System.out.println("  ----------");
        System.out.println(this.buyBook);
        System.out.println("---------- // match engine ----------");
    }
}
