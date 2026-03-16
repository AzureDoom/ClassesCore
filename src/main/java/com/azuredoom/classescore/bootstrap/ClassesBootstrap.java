package com.azuredoom.classescore.bootstrap;

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
import com.azuredoom.classescore.data.ClassDefinition;
import com.azuredoom.classescore.data.ClassRegistry;
import com.azuredoom.classescore.data.EquipmentRules;
import com.azuredoom.classescore.data.PassiveDefinition;
import com.azuredoom.classescore.data.PassiveType;
import com.azuredoom.classescore.db.JdbcClassesRepository;
import com.azuredoom.classescore.service.ClassServiceImpl;

public final class ClassesBootstrap {

    private static final Gson GSON = new Gson();

    private final ClassesCore plugin;

    private final ClassesCoreConfig config;

    public ClassesBootstrap(ClassesCore plugin, ClassesCoreConfig config) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.config = Objects.requireNonNull(config, "config");
    }

    public BootstrapResult bootstrap() {
        var dataSource = new JdbcDataSource();
        dataSource.setURL(config.getJDBCConnection());
        dataSource.setUser("");
        dataSource.setPassword("");

        var repository = new JdbcClassesRepository(dataSource, "classescore_");
        repository.initializeSchema();

        var registry = new ClassRegistry();
        loadAllClasses(registry);

        var service = new ClassServiceImpl(repository, registry, ClassesCore.getPlayerRestrictionCache());
        return new BootstrapResult(repository, registry, service, repository::close);
    }

    private void loadAllClasses(ClassRegistry registry) {
        try {
            var mergedDefinitions = new LinkedHashMap<String, ClassDefinition>();
            loadAllClasspathClasses(mergedDefinitions);
            loadExternalZipAssetPacks(mergedDefinitions);
            for (var definition : mergedDefinitions.values()) {
                registry.register(definition);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load class definitions", e);
        }
    }

    private void loadAllClasspathClasses(Map<String, ClassDefinition> sink) throws Exception {
        var classLoader = plugin.getClass().getClassLoader();
        Enumeration<URL> resources = classLoader.getResources("classes");

        if (!resources.hasMoreElements()) {
            throw new IllegalStateException("Missing classes resource folder");
        }

        while (resources.hasMoreElements()) {
            var resourceUrl = resources.nextElement();
            var protocol = resourceUrl.getProtocol();

            if ("file".equals(protocol)) {
                loadClassesFromDirectory(sink, resourceUrl);
            } else if ("jar".equals(protocol)) {
                loadClassesFromJar(sink, resourceUrl);
            } else {
                plugin.getLogger()
                    .atWarning()
                    .log("Skipping unsupported classes resource protocol: " + protocol + " (" + resourceUrl + ")");
            }
        }
    }

    private void loadClassesFromDirectory(Map<String, ClassDefinition> sink, URL resourceUrl) throws Exception {
        var classesPath = Paths.get(resourceUrl.toURI());

        try (var stream = Files.walk(classesPath)) {
            stream.filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".json"))
                .forEach(path -> {
                    var relative = classesPath.relativize(path).toString().replace('\\', '/');
                    var sourceName = "classes/" + relative;

                    try (var input = Files.newInputStream(path)) {
                        var definition = loadClass(input, sourceName);
                        putDefinition(sink, definition, false, sourceName);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to load class resource " + sourceName, e);
                    }
                });
        }
    }

    private void loadClassesFromJar(Map<String, ClassDefinition> sink, URL resourceUrl) throws Exception {
        var connection = (JarURLConnection) resourceUrl.openConnection();

        try (var jarFile = connection.getJarFile()) {
            var entries = jarFile.entries();

            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                var name = entry.getName();

                if (!entry.isDirectory() && name.startsWith("classes/") && name.endsWith(".json")) {
                    try (var input = jarFile.getInputStream(entry)) {
                        var definition = loadClass(input, name);
                        putDefinition(sink, definition, false, name);
                    }
                }
            }
        }
    }

    private void loadExternalZipAssetPacks(Map<String, ClassDefinition> sink) throws Exception {
        var assetPackDir = resolveAssetPackDirectory();

        if (!Files.exists(assetPackDir) || !Files.isDirectory(assetPackDir)) {
            return;
        }

        try (var stream = Files.list(assetPackDir)) {
            stream.filter(Files::isRegularFile)
                .filter(path -> {
                    var name = path.getFileName().toString().toLowerCase();
                    return name.endsWith(".zip") || name.endsWith(".jar");
                })
                .sorted()
                .forEach(path -> {
                    try {
                        loadClassesFromZip(sink, path);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to load asset pack " + path, e);
                    }
                });
        }
    }

    private void loadClassesFromZip(Map<String, ClassDefinition> sink, Path zipPath) throws Exception {
        try (var zipFile = new ZipFile(zipPath.toFile())) {
            var entries = zipFile.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                var name = entry.getName();

                if (!entry.isDirectory() && name.startsWith("classes/") && name.endsWith(".json")) {
                    try (var input = zipFile.getInputStream(entry)) {
                        var definition = loadClass(input, zipPath.getFileName() + "!/" + name);
                        putDefinition(sink, definition, true, zipPath + "!/" + name);
                    }
                }
            }
        }
    }

    private void putDefinition(
        Map<String, ClassDefinition> sink,
        ClassDefinition definition,
        boolean overrideExisting,
        String sourceName
    ) {
        Objects.requireNonNull(definition, "definition");

        var id = definition.id();
        if (id == null || id.isBlank()) {
            throw new IllegalStateException("Class definition from " + sourceName + " has null or blank id");
        }

        var existing = sink.get(id);
        if (existing == null) {
            sink.put(id, definition);
            plugin.getLogger().at(Level.INFO).log("Loaded class '" + id + "' from " + sourceName);
            return;
        }

        if (overrideExisting) {
            sink.put(id, definition);
            plugin.getLogger().at(Level.INFO).log("Overrode class '" + id + "' from " + sourceName);
        } else {
            plugin.getLogger()
                .atWarning()
                .log(
                    "Skipping duplicate built-in/classpath class '" + id + "' from " + sourceName
                        + " because one was already loaded"
                );
        }
    }

    private ClassDefinition loadClass(InputStream stream, String sourceName) {
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            var root = GSON.fromJson(reader, JsonObject.class);

            if (root == null) {
                throw new IllegalStateException("Class JSON was empty: " + sourceName);
            }

            var id = root.get("id").getAsString();
            var displayName = root.get("displayName").getAsString();
            var description = root.get("description").getAsString();

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
                passives,
                new EquipmentRules(allowedWeapons, allowedArmor)
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to load class resource " + sourceName, e);
        }
    }

    private ClassDefinition loadClass(String resourcePath) {
        try (var stream = plugin.getClass().getResourceAsStream(resourcePath)) {
            if (stream == null) {
                throw new IllegalStateException("Missing class resource: " + resourcePath);
            }
            return loadClass(stream, resourcePath);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load class resource " + resourcePath, e);
        }
    }

    private Path resolveAssetPackDirectory() {
        return Paths.get("mods").toAbsolutePath().normalize();
    }

    public record BootstrapResult(
        JdbcClassesRepository repository,
        ClassRegistry registry,
        ClassServiceImpl service,
        AutoCloseable closeable
    ) {}
}
