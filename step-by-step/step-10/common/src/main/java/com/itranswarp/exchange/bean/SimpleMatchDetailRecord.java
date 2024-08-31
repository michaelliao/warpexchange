package com.itranswarp.exchange.bean;

import java.math.BigDecimal;

import com.itranswarp.exchange.enums.MatchType;

public record SimpleMatchDetailRecord(BigDecimal price, BigDecimal quantity, MatchType type) {
}
