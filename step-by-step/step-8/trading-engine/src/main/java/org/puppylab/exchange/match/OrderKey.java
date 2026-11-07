package org.puppylab.exchange.match;

import java.math.BigDecimal;

public record OrderKey(long sequenceId, BigDecimal price) {
}
