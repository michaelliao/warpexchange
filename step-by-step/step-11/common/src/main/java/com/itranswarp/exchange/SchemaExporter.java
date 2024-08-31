package com.itranswarp.exchange;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.itranswarp.exchange.db.DbTemplate;

public class SchemaExporter {

    public static void main(String[] args) throws IOException {
        DbTemplate dbTemplate = new DbTemplate(null);
        String ddl = """
                -- init exchange database

                DROP DATABASE IF EXISTS exchange;

                CREATE DATABASE exchange;

                USE exchange;

                """;
        ddl = ddl + dbTemplate.exportDDL();
        System.out.println(ddl);
        Path path = Path.of(".").toAbsolutePath().getParent().getParent().resolve("build").resolve("sql")
                .resolve("schema.sql");
        Files.writeString(path, ddl);
        System.out.println("mysql -u root --password=password < " + path);
    }
}
