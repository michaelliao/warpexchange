package org.puppylab.exchange.bean;

import java.math.BigDecimal;

import org.puppylab.exchange.enums.MatchType;

public record SimpleMatchDetailRecord(BigDecimal price, BigDecimal quantity, MatchType type) {
}
