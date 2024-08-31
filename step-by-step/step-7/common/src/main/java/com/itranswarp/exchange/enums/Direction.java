package com.itranswarp.exchange.enums;

public enum Direction {

    BUY(1),

    SELL(0);

    /**
     * Direction的int值
     */
    public final int value;

    /**
     * Get negate direction.
     */
    public Direction negate() {
        return this == BUY ? SELL : BUY;
    }

    Direction(int value) {
        this.value = value;
    }

    public static Direction of(int intValue) {
        if (intValue == 1) {
            return BUY;
        }
        if (intValue == 0) {
            return SELL;
        }
        throw new IllegalArgumentException("Invalid Direction value.");
    }
}
