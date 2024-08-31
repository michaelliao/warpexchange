package com.itranswarp.exchange.match;

import java.math.BigDecimal;

import com.itranswarp.exchange.model.trade.OrderEntity;

public record MatchDetailRecord(BigDecimal price, BigDecimal quantity, OrderEntity takerOrder, OrderEntity makerOrder) {
}
