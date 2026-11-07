package org.puppylab.exchange.match;

import java.math.BigDecimal;

import org.puppylab.exchange.model.trade.OrderEntity;

public record MatchDetailRecord(BigDecimal price, BigDecimal quantity, OrderEntity takerOrder, OrderEntity makerOrder) {
}
