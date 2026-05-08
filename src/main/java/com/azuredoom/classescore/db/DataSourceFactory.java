package com.azuredoom.classescore.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public final class DataSourceFactory {

    private DataSourceFactory() {}

    public static HikariDataSource create(String jdbcUrl, int maxPoolSize) {
        validate(jdbcUrl, maxPoolSize);

        var cfg = new HikariConfig();
        cfg.setJdbcUrl(jdbcUrl);
        cfg.setDriverClassName(driverClassNameFor(jdbcUrl));

        cfg.setMaximumPoolSize(maxPoolSize);
        cfg.setMinimumIdle(1);

        cfg.setInitializationFailTimeout(10_000);
        cfg.setConnectionTimeout(10_000);
        cfg.setValidationTimeout(5_000);

        try {
            return new HikariDataSource(cfg);
        } catch (RuntimeException e) {
            throw new IllegalStateException(
                "Failed to initialize database connection pool for: " + sanitize(jdbcUrl),
                e
            );
        }
    }

    private static void validate(String jdbcUrl, int maxPoolSize) {
        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            throw new IllegalArgumentException(
                "jdbcUrl is missing/blank. Example: jdbc:postgresql://host:5432/db?user=name&password=secret"
            );
        }

        var lower = jdbcUrl.toLowerCase();

        if (!lower.startsWith("jdbc:")) {
            throw new IllegalArgumentException("jdbcUrl must start with 'jdbc:'");
        }

        if (lower.startsWith("jdbc:postgresql://") && lower.matches("^jdbc:postgresql://[^/@:]+:[^/@]+@.+")) {
            throw new IllegalArgumentException(
                "PostgreSQL JDBC URLs must not use user:password@host syntax. " +
                    "Use jdbc:postgresql://host:5432/db?user=name&password=secret"
            );
        }

        if (!isH2(lower) && !hasCredentialsInUrl(lower)) {
            throw new IllegalArgumentException(
                "Database credentials are missing. Use ?user=...&password=... in the JDBC URL."
            );
        }

        if (maxPoolSize < 1) {
            throw new IllegalArgumentException("maxPoolSize must be >= 1");
        }
    }

    private static boolean hasCredentialsInUrl(String lowerJdbcUrl) {
        var q = lowerJdbcUrl.indexOf('?');
        if (q < 0) {
            return false;
        }

        var query = lowerJdbcUrl.substring(q + 1);

        return containsQueryParam(query, "user")
            || containsQueryParam(query, "username")
            || containsQueryParam(query, "password");
    }

    private static boolean containsQueryParam(String query, String key) {
        return query.startsWith(key + "=") || query.contains("&" + key + "=");
    }

    private static boolean isH2(String lowerJdbcUrl) {
        return lowerJdbcUrl.startsWith("jdbc:h2:");
    }

    private static String driverClassNameFor(String jdbcUrl) {
        var lower = jdbcUrl.toLowerCase();

        if (lower.startsWith("jdbc:postgresql:")) {
            if (classExists("com.azuredoom.classescore.libs.postgresql.Driver")) {
                return "com.azuredoom.classescore.libs.postgresql.Driver";
            }
            return "org.postgresql.Driver";
        }

        if (lower.startsWith("jdbc:mysql:")) {
            if (classExists("com.azuredoom.classescore.libs.mysql.cj.jdbc.Driver")) {
                return "com.azuredoom.classescore.libs.mysql.cj.jdbc.Driver";
            }
            return "com.mysql.cj.jdbc.Driver";
        }

        if (lower.startsWith("jdbc:mariadb:")) {
            if (classExists("com.azuredoom.classescore.libs.mariadb.jdbc.Driver")) {
                return "com.azuredoom.classescore.libs.mariadb.jdbc.Driver";
            }
            return "org.mariadb.jdbc.Driver";
        }

        if (lower.startsWith("jdbc:h2:")) {
            if (classExists("com.azuredoom.classescore.libs.h2.Driver")) {
                return "com.azuredoom.classescore.libs.h2.Driver";
            }
            return "org.h2.Driver";
        }

        throw new IllegalArgumentException(
            "Unsupported JDBC URL scheme. Supported: postgresql, mysql, mariadb, h2. Got: " + sanitize(jdbcUrl)
        );
    }

    private static boolean classExists(String name) {
        try {
            Class.forName(name, false, DataSourceFactory.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static String sanitize(String jdbcUrl) {
        if (jdbcUrl == null) {
            return null;
        }

        return jdbcUrl
            .replaceAll("(?i)([?&]password=)[^&]*", "$1****")
            .replaceAll("(?i)([?&]user=)[^&]*", "$1****")
            .replaceAll("(?i)([?&]username=)[^&]*", "$1****");
    }
}
