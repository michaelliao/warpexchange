package com.itranswarp.exchange.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class ClassPathUtil {

    public static String readFile(String classPathFile) throws IOException {
        try (InputStream input = ClassPathUtil.class.getResourceAsStream(classPathFile)) {
            if (input == null) {
                throw new IOException("Classpath file not found: " + classPathFile);
            }
            return readAsString(input);
        }
    }

    static byte[] readAsBytes(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream(1024 * 1024);
        input.transferTo(output);
        return output.toByteArray();
    }

    static String readAsString(InputStream input) throws IOException {
        return new String(readAsBytes(input), StandardCharsets.UTF_8);
    }
}
