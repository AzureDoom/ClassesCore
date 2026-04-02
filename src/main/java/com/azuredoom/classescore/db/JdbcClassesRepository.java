package com.azuredoom.classescore.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import javax.sql.DataSource;

import com.azuredoom.classescore.ClassesCore;
import com.azuredoom.classescore.api.model.PlayerClassState;
import com.azuredoom.classescore.exceptions.ClassesCoreException;

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

    /**
     * Initializes the schema for the ClassesCore database.
     * <p>
     * Throws: - {@code RuntimeException} if a {@code SQLException} occurs during schema initialization.
     */
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

    /**
     * Retrieves the player's class state based on the provided player ID. Queries the database to find a matching
     * record for the given UUID and maps it to a PlayerClassState object.
     *
     * @param playerId the unique identifier of the player whose class state is to be retrieved; must not be null
     * @return an {@code Optional} containing the {@code PlayerClassState} if a matching record is found, or
     *         {@code Optional.empty()} if no record exists for the provided player ID
     * @throws NullPointerException if {@code playerId} is null
     * @throws RuntimeException     if a database error occurs during the query execution
     */
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

    /**
     * Saves the provided player state to the database. This method checks whether the player state for the given player
     * ID already exists. If it exists, it updates the existing record; otherwise, it inserts a new record. The
     * operation is performed within a transaction to ensure atomicity.
     *
     * @param state the player state to be saved. The state must not be null, and it must contain a valid player ID.
     * @return the saved player state after successful persistence.
     * @throws NullPointerException if the provided state is null.
     * @throws RuntimeException     if a database connection cannot be established, the transaction fails, or any error
     *                              occurs while saving the player state.
     */
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

    /**
     * Deletes the state of a player identified by their unique ID from the database.
     *
     * @param playerId The unique identifier of the player whose state is to be deleted. Must not be null.
     * @throws NullPointerException If the provided {@code playerId} is null.
     * @throws RuntimeException     If an SQLException occurs while attempting to delete the player's state.
     */
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

    /**
     * Checks if a player's state exists in the database.
     *
     * @param connection The active database connection used to check for the player's state. Must not be null and must
     *                   be properly managed by the caller.
     * @param playerId   The unique identifier of the player to check. Must not be null.
     * @return {@code true} if the player's state exists in the database, {@code false} otherwise.
     * @throws SQLException If an error occurs while executing the database query.
     */
    private boolean playerStateExists(Connection connection, UUID playerId) throws SQLException {
        var sql = "SELECT 1 FROM %s WHERE player_uuid = ?".formatted(playerClassTable);

        try (var ps = connection.prepareStatement(sql)) {
            ps.setString(1, playerId.toString());
            try (var rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Inserts a player's class state into the database.
     *
     * @param connection The active database connection used to execute the insert statement. Must not be null and must
     *                   be properly managed by the caller.
     * @param state      The {@code PlayerClassState} containing the player's class state data to be inserted. Must not
     *                   be null and must contain valid values for the player's unique ID, class ID, creation timestamp,
     *                   and updated timestamp.
     * @throws SQLException If an error occurs while executing the database insert operation.
     */
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

    /**
     * Updates the state of a player's selected class in the database.
     *
     * @param connection The active database connection used to execute the update statement. Must not be null and must
     *                   be properly managed by the caller.
     * @param state      The {@code PlayerClassState} containing the new data to be updated. Must not be null and must
     *                   contain valid values for the player's unique ID, class ID, and the updated timestamp.
     * @throws SQLException If an error occurs while executing the database update.
     */
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

    /**
     * Maps the current row of the provided {@code ResultSet} to a {@code PlayerClassState} object.
     *
     * @param rs The {@code ResultSet} containing player class state data. Must be positioned at a valid row. Expected
     *           columns include: - "player_uuid" (VARCHAR): The unique identifier of the player. - "class_id"
     *           (VARCHAR): The unique identifier of the player's class. - "created_at" (BIGINT): The timestamp of the
     *           initial class selection. - "updated_at" (BIGINT): The timestamp of the last update to the class
     *           selection.
     * @return A {@code PlayerClassState} object created from the data in the current row of the {@code ResultSet}.
     * @throws SQLException If an SQL error occurs while accessing the {@code ResultSet}.
     */
    private PlayerClassState mapPlayerState(ResultSet rs) throws SQLException {
        return new PlayerClassState(
            UUID.fromString(rs.getString("player_uuid")),
            rs.getString("class_id"),
            rs.getLong("created_at"),
            rs.getLong("updated_at")
        );
    }

    /**
     * Constructs a SQL query string to create the player class table, ensuring its existence if it does not already
     * exist. The table will store information about player class assignments, with each row representing a player's
     * selected class.
     *
     * @return A formatted SQL string to create the player class table with the following columns: - `player_uuid`
     *         (VARCHAR of a predefined max length) as the primary key, representing the player's unique ID. -
     *         `class_id` (VARCHAR of a predefined max length) as a required column, representing the unique ID of the
     *         class. - `created_at` (BIGINT) as a required column, representing the timestamp of the initial class
     *         selection. - `updated_at` (BIGINT) as a required column, representing the timestamp of the last update to
     *         the class selection.
     */
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

    /**
     * Constructs a SQL query string to create the metadata table, ensuring its existence if it does not already exist.
     * The table will store metadata associated with players, with each row representing a unique metadata key-value
     * pair for a specific player identified by their UUID.
     *
     * @return A formatted SQL string to create the metadata table with the specified columns: - `player_uuid` (VARCHAR
     *         of a predefined max length) as the primary key, representing the player's unique ID. - `meta_key`
     *         (VARCHAR of a predefined max length) as part of the primary key, representing the metadata key. -
     *         `meta_value` (TEXT) as a required column, representing the value associated with the metadata key.
     */
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

    /**
     * Validates the state of the given {@code PlayerClassState} instance. Ensures that all required fields in the state
     * are non-null and meet the expected criteria.
     *
     * @param state The {@code PlayerClassState} to validate. Must not be null. The {@code playerId} field must not be
     *              null. The {@code classId} field must not be null or blank.
     * @throws IllegalArgumentException If any of the required fields in {@code state} are invalid.
     */
    private static void validatePlayerState(PlayerClassState state) {
        if (state.playerId() == null) {
            throw new IllegalArgumentException("playerId cannot be null");
        }
        if (state.classId() == null || state.classId().isBlank()) {
            throw new IllegalArgumentException("classId cannot be null or blank");
        }
    }

    /**
     * Normalizes the given table prefix by ensuring it is not null or blank. If the provided prefix is null or consists
     * solely of whitespace, a default prefix "classescore_" is returned.
     *
     * @param prefix The table prefix to normalize. It can be null, blank, or non-blank.
     * @return A normalized table prefix. If the input is null or blank, the default prefix "classescore_" is returned.
     *         Otherwise, the original prefix is returned.
     */
    private static String normalizePrefix(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return "classescore_";
        }
        return prefix;
    }

    /**
     * Rolls back the transaction on the given database connection. If an {@link SQLException} occurs during the
     * rollback, it is caught and ignored.
     *
     * @param connection The database connection on which the rollback operation will be performed. Must not be null;
     *                   the caller is responsible for ensuring a valid and open connection.
     */
    private static void rollbackQuietly(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException ignored) {}
    }

    /**
     * Closes the underlying JDBC datasource associated with the repository.
     *
     * @throws ClassesCoreException if the `dataSource` cannot be closed due to an underlying error.
     */
    public void close() {
        try {
            if (dataSource instanceof AutoCloseable c) {
                ClassesCore.LOGGER.at(Level.INFO).log("Closing JDBC datasource");
                c.close();
            }
        } catch (Exception e) {
            throw new ClassesCoreException("Failed to close JDBC datasource", e);
        }
    }
}
