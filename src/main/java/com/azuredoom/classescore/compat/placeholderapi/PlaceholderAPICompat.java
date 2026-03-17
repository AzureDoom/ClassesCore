package com.azuredoom.classescore.compat.placeholderapi;

public class PlaceholderAPICompat {

    private PlaceholderAPICompat() {}

    public static void register() {
        new ClassesCoreExpansion().register();
    }
}
