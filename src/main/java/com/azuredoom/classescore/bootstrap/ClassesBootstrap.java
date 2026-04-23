package com.azuredoom.classescore.bootstrap;

import com.azuredoom.hytalecustomassetloader.AssetDiscoveryOptions;
import com.azuredoom.hytalecustomassetloader.AssetLoader;
import com.azuredoom.hytalecustomassetloader.model.AssetSource;
import com.azuredoom.hytalecustomassetloader.model.AssetSourceKind;
import com.azuredoom.hytalecustomassetloader.spi.AssetLogger;
import com.azuredoom.hytalecustomassetloader.spi.ReloadableAssetRegistrar;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.h2.jdbcx.JdbcDataSource;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;

import com.azuredoom.classescore.ClassesCore;
import com.azuredoom.classescore.config.ClassesCoreConfig;
import com.azuredoom.classescore.data.*;
import com.azuredoom.classescore.db.JdbcClassesRepository;
import com.azuredoom.classescore.service.ClassServiceImpl;
import com.azuredoom.classescore.util.StatType;

public final class ClassesBootstrap {

    private static final Gson GSON = new Gson();

    private final ClassesCoreConfig config;

    private final ClassRegistry registry;

    private final AssetLoader<ClassDefinition> loader;

    private final ReloadableAssetRegistrar<ClassDefinition> registrar;

    public ClassesBootstrap(ClassesCore plugin, ClassesCoreConfig config) {
        this.config = Objects.requireNonNull(config, "config");
        this.registry = new ClassRegistry();
        var options = new AssetDiscoveryOptions(
            "classes",
            ".json",
            Paths.get("mods").toAbsolutePath().normalize(),
            true,
            true,
            true,
            true,
            true,
            Duration.ofMillis(250)
        );
        this.loader = new AssetLoader<>(
            plugin.getClass().getClassLoader(),
            options,
            this::loadClass,
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

        this.registrar = new ReloadableAssetRegistrar<>() {

            @Override
            public void add(String id, ClassDefinition asset) {
                registry.register(asset);
                plugin.getLogger().atInfo().log("Added tag: " + id);
            }

            @Override
            public void update(String id, ClassDefinition previousAsset, ClassDefinition currentAsset) {
                registry.remove(id);
                registry.register(currentAsset);
                plugin.getLogger().atInfo().log("Updated tag: " + id);
            }

            @Override
            public void remove(String id, ClassDefinition asset) {
                registry.remove(id);
                plugin.getLogger().atInfo().log("Removed tag: " + id);
            }
        };
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
        var result = loader.loadAll();

        for (var entry : result.snapshot().mergedAssets().entrySet()) {
            registrar.add(entry.getKey(), entry.getValue());
        }

        var service = new ClassServiceImpl(repository, registry, ClassesCore.getPlayerRestrictionCache());
        return new BootstrapResult(repository, registry, service, repository::close);
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
    private ClassDefinition loadClass(InputStream stream, String sourceName, AssetSourceKind sourceKind) {
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
                new EquipmentRules(allowedWeapons, allowedArmor),
                new AssetSource(sourceKind, sourceName)
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to load class resource " + sourceName, e);
        }
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
