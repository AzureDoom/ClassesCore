package com.azuredoom.classescore.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public class ClassesConfig {

    public static final BuilderCodec<ClassesConfig> CODEC = BuilderCodec.builder(
        ClassesConfig.class,
        ClassesConfig::new
    )
        .append(
            new KeyedCodec<>("test", Codec.BOOLEAN),
            (exConfig, aBoolean, _) -> exConfig.test = aBoolean,
            (exConfig, _) -> exConfig.test
        )
        .add()
        .build();

    public boolean test = false;

    public ClassesConfig() {}

    public boolean isTest() {
        return test;
    }
}
