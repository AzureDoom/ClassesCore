package com.azuredoom.classescore.bootstrap;

import com.azuredoom.hytalecustomassetloader.AssetDiscoveryOptions;
import com.azuredoom.hytalecustomassetloader.AssetLoader;
import com.azuredoom.hytalecustomassetloader.spi.AssetLogger;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.h2.jdbcx.JdbcDataSource;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.azuredoom.classescore.ClassesCore;
import com.azuredoom.classescore.config.ClassesCoreConfig;
import com.azuredoom.classescore.data.*;
import com.azuredoom.classescore.db.JdbcClassesRepository;
import com.azuredoom.classescore.service.ClassServiceImpl;
import com.azuredoom.classescore.util.StatType;

public final class ClassesBootstrap {

    private static final Gson GSON = new Gson();

    private final ClassesCore plugin;

    private final ClassesCoreConfig config;

    public ClassesBootstrap(ClassesCore plugin, ClassesCoreConfig config) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.config = Objects.requireNonNull(config, "config");
    }

    /**
     * Initializes and bootstraps the system by setting up the required data sources, repositories, registries, and
     * services. It also prepares the schema for use and loads all available class definitions into the registry.
     *
     * @return A {@link BootstrapResult} object containing the initialized repository, registry, service, and a cleanup
     *         operation to release resources.
     */
    public BootstrapResult bootstrap() {
        var dataSource = new JdbcDataSource();
        dataSource.setURL(config.getJDBCConnection());
        dataSource.setUser("");
        dataSource.setPassword("");

        var repository = new JdbcClassesRepository(dataSource);
        repository.initializeSchema();

        var registry = new ClassRegistry();
        loadAllClasses(registry);

        var service = new ClassServiceImpl(repository, registry, ClassesCore.getPlayerRestrictionCache());
        return new BootstrapResult(repository, registry, service, repository::close);
    }

    /**
     * Loads all class definitions from available sources and registers them into the provided {@link ClassRegistry}.
     * This method aggregates class definitions from the classpath and external ZIP asset packs, merges them, and
     * registers each unique definition in the registry.
     *
     * @param registry The {@link ClassRegistry} instance where the loaded class definitions will be registered. Must
     *                 not be null.
     * @throws RuntimeException If any error occurs during the loading or registration of class definitions.
     */
    private void loadAllClasses(ClassRegistry registry) {
        try {
            var loader = new AssetLoader<>(
                    plugin.getClass().getClassLoader(),
                    new AssetDiscoveryOptions(
                            "classes",
                            ".json",
                            resolveAssetPackDirectory(),
                            true,
                            true
                    ),
                    (stream, sourceName, sourceKind) -> loadClass(stream, sourceName),
                    ClassDefinition::id,
                    new AssetLogger() {
                        @Override
                        public void info(String message) {
                            plugin.getLogger().atInfo().log(message);
                        }

                        @Override
                        public void warn(String message) {
                            plugin.getLogger().atWarning().log(message);
                        }
                    }
            );

            var result = loader.loadAll();

            for (var definition : result.mergedAssets().values()) {
                registry.register(definition);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load class definitions", e);
        }
    }

    /**
     * Loads a class definition from a given JSON input stream and source identifier. This method parses the provided
     * input stream to extract class attributes, including its unique identifier, display name, description, statistics,
     * passive abilities, and equipment rules.
     *
     * @param stream     The {@link InputStream} containing the JSON representation of the class definition.
     * @param sourceName The string identifier of the source, used for error reporting purposes.
     * @return A {@link ClassDefinition} object containing the parsed class data.
     * @throws RuntimeException If an error occurs while reading or parsing the input, or if the JSON structure is
     *                          invalid.
     */
    private ClassDefinition loadClass(InputStream stream, String sourceName) {
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            var root = GSON.fromJson(reader, JsonObject.class);

            if (root == null) {
                throw new IllegalStateException("Class JSON was empty: " + sourceName);
            }

            var id = root.get("id").getAsString();
            var displayName = root.get("displayName").getAsString();
            var description = root.get("description").getAsString();

            var stats = new ArrayList<StatDefinition>();
            var statsJson = root.getAsJsonArray("stats");

            if (statsJson != null) {
                for (var element : statsJson) {
                    var statObj = element.getAsJsonObject();

                    if (!statObj.has("id")) {
                        throw new IllegalStateException("Stat missing 'id' in " + sourceName);
                    }
                    var statId = statObj.get("id").getAsString();

                    StatType.fromJson(statId);

                    var base = statObj.has("base") ? statObj.get("base").getAsInt() : 0;
                    var perLevel = statObj.has("perLevel") ? statObj.get("perLevel").getAsInt() : 0;

                    if (base < 0 || perLevel < 0) {
                        throw new IllegalStateException("Negative stat values not allowed: " + statId);
                    }
                    stats.add(new StatDefinition(statId, base, perLevel));
                }
            }
            var passives = new ArrayList<PassiveDefinition>();
            var passivesJson = root.getAsJsonArray("passives");
            if (passivesJson != null) {
                for (var element : passivesJson) {
                    var passive = element.getAsJsonObject();
                    passives.add(
                        new PassiveDefinition(
                            passive.get("id").getAsString(),
                            PassiveType.fromJson(passive.get("type").getAsString()),
                            passive.has("attribute") ? passive.get("attribute").getAsString() : null,
                            passive.has("damageType") ? passive.get("damageType").getAsString() : null,
                            passive.get("value").getAsFloat()
                        )
                    );
                }
            }

            var allowedWeapons = new HashSet<String>();
            var allowedArmor = new HashSet<String>();
            var equipmentRulesJson = root.getAsJsonObject("equipmentRules");

            if (equipmentRulesJson != null && equipmentRulesJson.has("allowedWeapons")) {
                for (var element : equipmentRulesJson.getAsJsonArray("allowedWeapons")) {
                    allowedWeapons.add(element.getAsString());
                }
            }

            if (equipmentRulesJson != null && equipmentRulesJson.has("allowedArmor")) {
                for (var element : equipmentRulesJson.getAsJsonArray("allowedArmor")) {
                    allowedArmor.add(element.getAsString());
                }
            }

            return new ClassDefinition(
                id,
                displayName,
                description,
                stats,
                passives,
                new EquipmentRules(allowedWeapons, allowedArmor)
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to load class resource " + sourceName, e);
        }
    }

    /**
     * Resolves the directory path where asset packs (such as .zip or .jar files) are stored. This method ensures that
     * the path is absolute and normalized for consistent usage.
     *
     * @return the {@link Path} object representing the absolute and normalized "mods" directory.
     */
    private Path resolveAssetPackDirectory() {
        return Paths.get("mods").toAbsolutePath().normalize();
    }

    /**
     * A record that encapsulates the result of the bootstrap process for managing class-related operations and
     * resources. It acts as a container for key parts involved in the system's class management and lifecycle control.
     *
     * @param repository The repository responsible for managing class data in the database.
     * @param registry   The registry for storing and managing loaded class definitions.
     * @param service    The service for handling higher-level class management operations.
     * @param closeable  A resource that can be closed to release associated system resources.
     */
    public record BootstrapResult(
        JdbcClassesRepository repository,
        ClassRegistry registry,
        ClassServiceImpl service,
        AutoCloseable closeable
    ) {}
}
