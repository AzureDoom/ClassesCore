package com.azuredoom.classescore.db;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;

import com.azuredoom.classescore.api.model.PlayerClassState;

public final class JdbcClassesRepository {

    private static final int DEFAULT_VARCHAR_ID = 64;

    private static final int DEFAULT_VARCHAR_META_KEY = 64;

    private static final int DEFAULT_VARCHAR_UUID = 36;

    private final DataSource dataSource;

    private final String playerClassTable;

    private final String metadataTable;

    public JdbcClassesRepository(DataSource dataSource) {
        this(dataSource, "classescore_");
    }

    public JdbcClassesRepository(DataSource dataSource, String tablePrefix) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
        String safePrefix = normalizePrefix(tablePrefix);

        this.playerClassTable = safePrefix + "player_class";
        this.metadataTable = safePrefix + "player_metadata";
    }

    public void initializeSchema() {
        try (
            Connection connection = dataSource.getConnection();
            Statement statement = connection.createStatement()
        ) {

            statement.execute(buildCreatePlayerClassTableSql());
            statement.execute(buildCreateMetadataTableSql());

        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize ClassesCore schema", e);
        }
    }

    public Optional<PlayerClassState> findPlayerState(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");

        var sql = """
            SELECT player_uuid, class_id, created_at, updated_at
            FROM %s
            WHERE player_uuid = ?
            """.formatted(playerClassTable);

        try (
            var connection = dataSource.getConnection();
            var ps = connection.prepareStatement(sql)
        ) {

            ps.setString(1, playerId.toString());

            try (var rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapPlayerState(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find player class state for " + playerId, e);
        }
    }

    public PlayerClassState savePlayerState(PlayerClassState state) {
        Objects.requireNonNull(state, "state");
        validatePlayerState(state);

        try (var connection = dataSource.getConnection()) {
            var previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            try {
                if (playerStateExists(connection, state.playerId())) {
                    updatePlayerState(connection, state);
                } else {
                    insertPlayerState(connection, state);
                }

                connection.commit();
                connection.setAutoCommit(previousAutoCommit);
                return state;
            } catch (SQLException e) {
                rollbackQuietly(connection);
                throw new RuntimeException(
                    "Failed to save player class state for " + state.playerId(),
                    e
                );
            }
        } catch (SQLException e) {
            throw new RuntimeException(
                "Failed to open connection while saving player class state for " + state.playerId(),
                e
            );
        }
    }

    public void deletePlayerState(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");

        var sql = "DELETE FROM %s WHERE player_uuid = ?".formatted(playerClassTable);

        try (
            Connection connection = dataSource.getConnection();
            PreparedStatement ps = connection.prepareStatement(sql)
        ) {

            ps.setString(1, playerId.toString());
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete player class state for " + playerId, e);
        }
    }

    public void saveMetadata(UUID playerId, String key, String valueJson) {
        Objects.requireNonNull(playerId, "playerId");
        validateKey(key);
        Objects.requireNonNull(valueJson, "valueJson");

        try (var connection = dataSource.getConnection()) {
            var previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            try {
                if (metadataExists(connection, playerId, key)) {
                    updateMetadata(connection, playerId, key, valueJson);
                } else {
                    insertMetadata(connection, playerId, key, valueJson);
                }

                connection.commit();
                connection.setAutoCommit(previousAutoCommit);
            } catch (SQLException e) {
                rollbackQuietly(connection);
                throw new RuntimeException(
                    "Failed to save metadata for player " + playerId + " and key " + key,
                    e
                );
            }
        } catch (SQLException e) {
            throw new RuntimeException(
                "Failed to open connection while saving metadata for player " + playerId,
                e
            );
        }
    }

    public Optional<String> findMetadata(UUID playerId, String key) {
        Objects.requireNonNull(playerId, "playerId");
        validateKey(key);

        var sql = """
            SELECT meta_value
            FROM %s
            WHERE player_uuid = ? AND meta_key = ?
            """.formatted(metadataTable);

        try (
            var connection = dataSource.getConnection();
            var ps = connection.prepareStatement(sql)
        ) {

            ps.setString(1, playerId.toString());
            ps.setString(2, key);

            try (var rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.ofNullable(rs.getString("meta_value"));
            }
        } catch (SQLException e) {
            throw new RuntimeException(
                "Failed to find metadata for player " + playerId + " and key " + key,
                e
            );
        }
    }

    public void deleteMetadata(UUID playerId, String key) {
        Objects.requireNonNull(playerId, "playerId");
        validateKey(key);

        var sql = """
            DELETE FROM %s
            WHERE player_uuid = ? AND meta_key = ?
            """.formatted(metadataTable);

        try (
            var connection = dataSource.getConnection();
            PreparedStatement ps = connection.prepareStatement(sql)
        ) {

            ps.setString(1, playerId.toString());
            ps.setString(2, key);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(
                "Failed to delete metadata for player " + playerId + " and key " + key,
                e
            );
        }
    }

    private boolean playerStateExists(Connection connection, UUID playerId) throws SQLException {
        var sql = "SELECT 1 FROM %s WHERE player_uuid = ?".formatted(playerClassTable);

        try (var ps = connection.prepareStatement(sql)) {
            ps.setString(1, playerId.toString());
            try (var rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private void insertPlayerState(Connection connection, PlayerClassState state) throws SQLException {
        var sql = """
            INSERT INTO %s (player_uuid, class_id, created_at, updated_at)
            VALUES (?, ?, ?, ?)
            """.formatted(playerClassTable);

        try (var ps = connection.prepareStatement(sql)) {
            ps.setString(1, state.playerId().toString());
            ps.setString(2, state.classId());
            ps.setLong(3, state.createdAt());
            ps.setLong(4, state.updatedAt());
            ps.executeUpdate();
        }
    }

    private void updatePlayerState(Connection connection, PlayerClassState state) throws SQLException {
        var sql = """
            UPDATE %s
            SET class_id = ?, updated_at = ?
            WHERE player_uuid = ?
            """.formatted(playerClassTable);

        try (var ps = connection.prepareStatement(sql)) {
            ps.setString(1, state.classId());
            ps.setLong(2, state.updatedAt());
            ps.setString(3, state.playerId().toString());
            ps.executeUpdate();
        }
    }

    private boolean metadataExists(Connection connection, UUID playerId, String key) throws SQLException {
        var sql = """
            SELECT 1
            FROM %s
            WHERE player_uuid = ? AND meta_key = ?
            """.formatted(metadataTable);

        try (var ps = connection.prepareStatement(sql)) {
            ps.setString(1, playerId.toString());
            ps.setString(2, key);
            try (var rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private void insertMetadata(
        Connection connection,
        UUID playerId,
        String key,
        String valueJson
    ) throws SQLException {
        var sql = """
            INSERT INTO %s (player_uuid, meta_key, meta_value)
            VALUES (?, ?, ?)
            """.formatted(metadataTable);

        try (var ps = connection.prepareStatement(sql)) {
            ps.setString(1, playerId.toString());
            ps.setString(2, key);
            ps.setString(3, valueJson);
            ps.executeUpdate();
        }
    }

    private void updateMetadata(
        Connection connection,
        UUID playerId,
        String key,
        String valueJson
    ) throws SQLException {
        var sql = """
            UPDATE %s
            SET meta_value = ?
            WHERE player_uuid = ? AND meta_key = ?
            """.formatted(metadataTable);

        try (var ps = connection.prepareStatement(sql)) {
            ps.setString(1, valueJson);
            ps.setString(2, playerId.toString());
            ps.setString(3, key);
            ps.executeUpdate();
        }
    }

    private PlayerClassState mapPlayerState(ResultSet rs) throws SQLException {
        return new PlayerClassState(
            UUID.fromString(rs.getString("player_uuid")),
            rs.getString("class_id"),
            rs.getLong("created_at"),
            rs.getLong("updated_at")
        );
    }

    private String buildCreatePlayerClassTableSql() {
        return """
            CREATE TABLE IF NOT EXISTS %s (
                player_uuid VARCHAR(%d) PRIMARY KEY,
                class_id VARCHAR(%d) NOT NULL,
                created_at BIGINT NOT NULL,
                updated_at BIGINT NOT NULL
            )
            """.formatted(
            playerClassTable,
            DEFAULT_VARCHAR_UUID,
            DEFAULT_VARCHAR_ID
        );
    }

    private String buildCreateMetadataTableSql() {
        return """
            CREATE TABLE IF NOT EXISTS %s (
                player_uuid VARCHAR(%d) NOT NULL,
                meta_key VARCHAR(%d) NOT NULL,
                meta_value TEXT NOT NULL,
                PRIMARY KEY (player_uuid, meta_key)
            )
            """.formatted(
            metadataTable,
            DEFAULT_VARCHAR_UUID,
            DEFAULT_VARCHAR_META_KEY
        );
    }

    private static void validatePlayerState(PlayerClassState state) {
        if (state.playerId() == null) {
            throw new IllegalArgumentException("playerId cannot be null");
        }
        if (state.classId() == null || state.classId().isBlank()) {
            throw new IllegalArgumentException("classId cannot be null or blank");
        }
    }

    private static void validateKey(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("metadata key cannot be null or blank");
        }
    }

    private static String normalizePrefix(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return "classescore_";
        }
        return prefix;
    }

    private static void rollbackQuietly(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException ignored) {}
    }

    public String describeDatabase() {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData meta = connection.getMetaData();
            return meta.getDatabaseProductName() + " " + meta.getDatabaseProductVersion();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to inspect database metadata", e);
        }
    }
}
