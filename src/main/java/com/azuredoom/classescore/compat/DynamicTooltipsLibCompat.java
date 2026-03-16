package com.azuredoom.classescore.compat;

import org.herolias.tooltips.api.DynamicTooltipsApiProvider;

import com.azuredoom.classescore.api.ClassesCoreAPI;

public class DynamicTooltipsLibCompat {

    private static boolean registered = false;

    private DynamicTooltipsLibCompat() {}

    public static void register() {
        if (registered)
            return;
        registered = true;

        var api = DynamicTooltipsApiProvider.get();
        if (api == null)
            return;

        var classesApi = ClassesCoreAPI.getClasses();

        api.addGlobalLine("", "");
    }
}
