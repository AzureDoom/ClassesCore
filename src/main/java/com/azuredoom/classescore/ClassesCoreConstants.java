package com.azuredoom.classescore;

import com.azuredoom.levelingcore.api.LevelingCoreApi;
import com.azuredoom.levelingcore.level.LevelServiceImpl;

public class ClassesCoreConstants {

    public static LevelServiceImpl getLevelingCoreApi() {
        var levelingCoreApi = LevelingCoreApi.getLevelServiceIfPresent();
        if (levelingCoreApi.isEmpty()) {
            throw new IllegalStateException("LevelingCore API is not available!");
        }
        return levelingCoreApi.get();
    }
}
