package com.azuredoom.classescore.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public class ClassesCoreConfig {

    public static final BuilderCodec<ClassesCoreConfig> CODEC = BuilderCodec.builder(
        ClassesCoreConfig.class,
        ClassesCoreConfig::new
    )
        .append(
            new KeyedCodec<>("JDBC_Connection", Codec.STRING),
            (exConfig, aString, _) -> exConfig.jdbcConnection = aString,
            (exConfig, _) -> exConfig.jdbcConnection
        )
        .add()
        .append(
            new KeyedCodec<>("Enable_Class_Item_Restrictions", Codec.BOOLEAN),
            (exConfig, aBoolean, _) -> exConfig.enableClassItemRestrictions = aBoolean,
            (exConfig, _) -> exConfig.enableClassItemRestrictions
        )
        .add()
        .build();

    private String jdbcConnection = "jdbc:h2:file:./mods/com.azuredoom_classescore/data/classescore;MODE=PostgreSQL";

    private boolean enableClassItemRestrictions = true;

    public String getJDBCConnection() {
        return jdbcConnection;
    }

    public boolean isEnableClassItemRestrictions() {
        return enableClassItemRestrictions;
    }
}
