package com.itranswarp.exchange.match;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.itranswarp.exchange.enums.Direction;
import com.itranswarp.exchange.enums.OrderStatus;
import com.itranswarp.exchange.model.trade.OrderEntity;

public class MatchEngineTest {

    static Long USER_A = 12345L;
    long sequenceId = 0;
    MatchEngine engine;

    @BeforeEach
    void init() {
        this.engine = new MatchEngine();
    }

    @Test
    void processOrders() {
        List<OrderEntity> orders = List.of( //
                createOrder(Direction.BUY, "12300.21", "1.02"), // 0
                createOrder(Direction.BUY, "12305.39", "0.33"), // 1
                createOrder(Direction.SELL, "12305.39", "0.11"), // 2
                createOrder(Direction.SELL, "12300.01", "0.33"), // 3
                createOrder(Direction.SELL, "12400.00", "0.10"), // 4
                createOrder(Direction.SELL, "12400.00", "0.20"), // 5
                createOrder(Direction.SELL, "12390.00", "0.15"), // 6
                createOrder(Direction.BUY, "12400.01", "0.55"), // 7
                createOrder(Direction.BUY, "12300.00", "0.77")); // 8
        List<MatchDetailRecord> matches = new ArrayList<>();
        for (OrderEntity order : orders) {
            MatchResult mr = this.engine.processOrder(order.sequenceId, order);
            matches.addAll(mr.matchDetails);
        }
        assertArrayEquals(new MatchDetailRecord[] { //
                new MatchDetailRecord(bd("12305.39"), bd("0.11"), orders.get(2), orders.get(1)), //
                new MatchDetailRecord(bd("12305.39"), bd("0.22"), orders.get(3), orders.get(1)), //
                new MatchDetailRecord(bd("12300.21"), bd("0.11"), orders.get(3), orders.get(0)), //
                new MatchDetailRecord(bd("12390.00"), bd("0.15"), orders.get(7), orders.get(6)), //
                new MatchDetailRecord(bd("12400.00"), bd("0.10"), orders.get(7), orders.get(4)), //
                new MatchDetailRecord(bd("12400.00"), bd("0.20"), orders.get(7), orders.get(5)), //
        }, matches.toArray(MatchDetailRecord[]::new));
        assertTrue(bd("12400.00").compareTo(engine.marketPrice) == 0);
    }

    OrderEntity createOrder(Direction direction, String price, String quantity) {
        this.sequenceId++;
        var order = new OrderEntity();
        order.id = this.sequenceId << 4;
        order.sequenceId = this.sequenceId;
        order.direction = direction;
        order.price = bd(price);
        order.quantity = order.unfilledQuantity = bd(quantity);
        order.status = OrderStatus.PENDING;
        order.userId = USER_A;
        order.createdAt = order.updatedAt = 1234567890000L + this.sequenceId;
        return order;
    }

    BigDecimal bd(String s) {
        return new BigDecimal(s);
    }

}
