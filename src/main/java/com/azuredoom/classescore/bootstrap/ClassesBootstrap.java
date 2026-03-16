package com.azuredoom.classescore.bootstrap;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.h2.jdbcx.JdbcDataSource;

import java.io.InputStreamReader;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;

import com.azuredoom.classescore.ClassesCore;
import com.azuredoom.classescore.config.ClassesCoreConfig;
import com.azuredoom.classescore.data.*;
import com.azuredoom.classescore.db.JdbcClassesRepository;
import com.azuredoom.classescore.service.ClassServiceImpl;

public final class ClassesBootstrap {

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
            var classLoader = plugin.getClass().getClassLoader();
            var resourceUrl = classLoader.getResource("classes");

            if (resourceUrl == null) {
                throw new IllegalStateException("Missing classes resource folder");
            }

            var protocol = resourceUrl.getProtocol();

            if ("file".equals(protocol)) {
                loadClassesFromDirectory(registry, resourceUrl);
            } else if ("jar".equals(protocol)) {
                loadClassesFromJar(registry, resourceUrl);
            } else {
                throw new IllegalStateException("Unsupported resource protocol for classes folder: " + protocol);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load class definitions", e);
        }
    }

    private void loadClassesFromDirectory(ClassRegistry registry, URL resourceUrl) throws Exception {
        var classesPath = Paths.get(resourceUrl.toURI());

        try (var stream = Files.walk(classesPath)) {
            stream.filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".json"))
                .forEach(path -> {
                    var relative = classesPath.relativize(path).toString().replace('\\', '/');
                    var resourcePath = "/classes/" + relative;
                    registry.register(loadClass(resourcePath));
                });
        }
    }

    private void loadClassesFromJar(ClassRegistry registry, URL resourceUrl) throws Exception {
        var connection = (JarURLConnection) resourceUrl.openConnection();

        try (var jarFile = connection.getJarFile()) {
            var entries = jarFile.entries();

            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                var name = entry.getName();

                if (name.startsWith("classes/") && name.endsWith(".json") && !entry.isDirectory()) {
                    registry.register(loadClass("/" + name));
                }
            }
        }
    }

    private ClassDefinition loadClass(String resourcePath) {
        try (var stream = plugin.getClass().getResourceAsStream(resourcePath)) {
            if (stream == null) {
                throw new IllegalStateException("Missing class resource: " + resourcePath);
            }
            var root = new Gson().fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), JsonObject.class);
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
            throw new RuntimeException("Failed to load class resource " + resourcePath, e);
        }
    }

    public record BootstrapResult(
        JdbcClassesRepository repository,
        ClassRegistry registry,
        ClassServiceImpl service,
        AutoCloseable closeable
    ) {}
}
