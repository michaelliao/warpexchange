package com.itranswarp.exchange.util;

import java.util.HexFormat;

public class ByteUtil {

    /**
     * Convert bytes to hex string (all lower-case).
     *
     * @param b Input bytes.
     * @return Hex string.
     */
    public static String toHexString(byte[] b) {
        return HexFormat.of().formatHex(b);
    }

    /**
     * Convert byte to hex string (all lower-case).
     *
     * @param b Input bytes.
     * @return Hex string.
     */
    public static String toHex(byte b) {
        return toHexString(new byte[] { b });
    }

}
