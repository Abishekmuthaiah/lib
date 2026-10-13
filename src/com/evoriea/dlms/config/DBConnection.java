package com.evoriea.dlms.config;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Singleton Database Connection Manager.
 * 
 * Implements the Singleton design pattern to maintain a single, controlled connection
 * lifecycle across the entire application runtime. Supports cross-RDBMS compatibility
 * for both SQLite (standalone embedded) and MySQL (multi-user client/server).
 * 
 * Architectural Layer 4: Database Connectivity Layer (Slide 6 & 9)
 */
public class DBConnection {
    private static volatile DBConnection instance;
    private Connection connection;

    // Default configuration: standalone SQLite database
    private static final String DEFAULT_SQLITE_FILE = "library_management.db";
    private static final String DEFAULT_SQLITE_URL = "jdbc:sqlite:" + DEFAULT_SQLITE_FILE;

    // Optional MySQL configuration via System properties or environment variables
    private String dbType; // "sqlite" or "mysql"
    private String jdbcUrl;
    private String dbUser;
    private String dbPassword;

    /**
     * Private constructor to enforce Singleton pattern.
     */
    private DBConnection() {
        this.dbType = System.getProperty("db.type", System.getenv().getOrDefault("DB_TYPE", "sqlite")).toLowerCase();
        
        if ("mysql".equals(dbType)) {
            String host = System.getProperty("db.host", System.getenv().getOrDefault("DB_HOST", "localhost"));
            String port = System.getProperty("db.port", System.getenv().getOrDefault("DB_PORT", "3306"));
            String dbName = System.getProperty("db.name", System.getenv().getOrDefault("DB_NAME", "library_management"));
            this.dbUser = System.getProperty("db.user", System.getenv().getOrDefault("DB_USER", "root"));
            this.dbPassword = System.getProperty("db.password", System.getenv().getOrDefault("DB_PASSWORD", ""));
            this.jdbcUrl = String.format("jdbc:mysql://%s:%s/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC", host, port, dbName);
        } else {
            this.dbType = "sqlite";
            String sqlitePath = System.getProperty("db.sqlite.path", DEFAULT_SQLITE_FILE);
            this.jdbcUrl = "jdbc:sqlite:" + sqlitePath;
        }
    }

    /**
     * Double-checked locking Singleton accessor.
     */
    public static DBConnection getInstance() {
        if (instance == null) {
            synchronized (DBConnection.class) {
                if (instance == null) {
                    instance = new DBConnection();
                }
            }
        }
        return instance;
    }

    /**
     * Retrieves an active JDBC Connection.
     * Reconnects automatically if the existing connection was closed or invalidated.
     */
    public synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                if ("mysql".equals(dbType)) {
                    Class.forName("com.mysql.cj.jdbc.Driver");
                    connection = DriverManager.getConnection(jdbcUrl, dbUser, dbPassword);
                } else {
                    Class.forName("org.sqlite.JDBC");
                    connection = DriverManager.getConnection(jdbcUrl);
                    // Crucial: SQLite requires foreign key constraints to be enabled explicitly
                    try (Statement stmt = connection.createStatement()) {
                        stmt.execute("PRAGMA foreign_keys = ON;");
                    }
                }
            } catch (ClassNotFoundException e) {
                throw new SQLException("JDBC Driver not found for " + dbType + ": " + e.getMessage(), e);
            }
        }
        return connection;
    }

    /**
     * Closes the singleton connection safely.
     */
    public synchronized void closeConnection() {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException ignored) {
            } finally {
                connection = null;
            }
        }
    }

    public String getDbType() {
        return dbType;
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public static String getDefaultSqliteFile() {
        return DEFAULT_SQLITE_FILE;
    }
}
