package com.itranswarp.exchange.util;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IdUtil {

    static Pattern STRING_ID_PATTERN = Pattern.compile("^[a-z0-9]{1,32}$");

    public static String generateUniqueId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static boolean isValidStringId(String id) {
        if (id == null || id.isEmpty()) {
            return false;
        }
        Matcher m = STRING_ID_PATTERN.matcher(id);
        return m.matches();
    }
}
