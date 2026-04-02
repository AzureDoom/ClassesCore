---
title: "Developer API"
order: 11
published: true
draft: false
---

This page explains how to include ClassesCore as a dependency in your Hytale plugin using Gradle and the AzureDoom Maven repository.

# Adding the Maven Repository

ClassesCore is hosted on the AzureDoom Maven.
Add the following repositories block to your build.gradle file:
```gradle
repositories {
    maven {
        name = "azuredoomMods"
        url = uri("https://maven.azuredoom.com/mods")
    }
}
```

------------------------------------------------------------------------

# Adding ClassesCore as a Dependency

Once the repository is added, include ClassesCore as a dependency:

```gradle
dependencies {
    implementation("com.azuredoom.classescore:Classescore:0.+")
}
```

Now in your mods/plugins `manifest.json` add `"com.azuredoom:classescore": "*"` to either `Dependencies` to set as hard or `OptionalDependencies` if you are adding integration like so:

```json
{
    // rest of manifest.json
    "Dependencies": {
        "com.azuredoom:classescore": "*"
    },
    "OptionalDependencies": {
        "com.azuredoom:classescore": "*"
    },
    // rest of manifest.json
}
```

------------------------------------------------------------------------

# Using the API

Sometimes you may want to directly access the underlying services.

## Getting the Class Service

``` java
ClassesCoreAPI.getClassServiceIfPresent().ifPresent(service -> {
    // interact with ClassServiceImpl
});
```

------------------------------------------------------------------------

## Getting the Class Registry

``` java
ClassesCoreAPI.getClassRegistryIfPresent().ifPresent(registry -> {
    registry.all().forEach(classDef -> {
        System.out.println(classDef.id());
    });
});
```

------------------------------------------------------------------------

### Getting All Registered Classes

``` java
Collection<ClassDefinition> classes = ClassesCoreAPI.getClasses();

for (ClassDefinition classDef : classes) {
    System.out.println(classDef.id());
}
```

------------------------------------------------------------------------

### Getting a Specific Class Definition

``` java
ClassesCoreAPI.getClassDefinition("mage").ifPresent(classDef -> {
    System.out.println("Class name: " + classDef.id());
});
```

------------------------------------------------------------------------

### Checking If a Class Exists

``` java
boolean exists = ClassesCoreAPI.hasClass("warrior");

if (exists) {
    System.out.println("Class exists!");
}
```

------------------------------------------------------------------------

### Check if a Player Has a Class

``` java
UUID playerId = player.getUuid();

if (ClassesCoreAPI.playerHasClass(playerId)) {
    System.out.println("Player has a class selected.");
}
```

------------------------------------------------------------------------

### Get Player Class State

``` java
ClassesCoreAPI.getPlayerState(playerId).ifPresent(state -> {
    System.out.println("Selected class ID: " + state.classId());
});
```

------------------------------------------------------------------------

### Get Selected Class ID

``` java
ClassesCoreAPI.getSelectedClassId(playerId).ifPresent(classId -> {
    System.out.println("Player class ID: " + classId);
});
```

### Get the Selected Class Definition

``` java
ClassesCoreAPI.getSelectedClass(playerId).ifPresent(classDef -> {
    System.out.println("Player class: " + classDef.id());
});
```

------------------------------------------------------------------------

### Selecting a Class

``` java
UUID playerId = player.getUuid();

boolean success = ClassesCoreAPI.selectClass(playerId, "mage");

if (success) {
    System.out.println("Class selected successfully!");
} else {
    System.out.println("Failed to select class.");
}
```

This method will fail if:

-   The class ID is null or blank
-   The class does not exist
-   The service is unavailable

------------------------------------------------------------------------

### Clearing a Player Class

``` java
UUID playerId = player.getUuid();

boolean success = ClassesCoreAPI.clearClass(playerId, "mage");

if (success) {
    System.out.println("Class cleared.");
}
```

------------------------------------------------------------------------

### Checking Weapon Usage

``` java
boolean canUse = ClassesCoreAPI.canUseWeapon(playerId, "diamond_sword");

if (!canUse) {
    System.out.println("Your class cannot use this weapon.");
}
```

### Checking Armor Usage

``` java
boolean canUse = ClassesCoreAPI.canUseArmor(playerId, "diamond_chestplate");

if (!canUse) {
    // Deny armor usage
}
```

### Display Player Class

``` java
ClassesCoreAPI.getSelectedClass(player.getUuid()).ifPresentOrElse(
    classDef -> {
        System.out.println("Your class: " + classDef.id());
    },
    () -> {
        System.out.println("You do not have a class selected.");
    }
);
```

------------------------------------------------------------------------

## Best Practices

### Prefer Optional Handling

Avoid calling `.get()` on Optionals.

Use:

``` java
ifPresent()
ifPresentOrElse()
map()
orElse()
```

------------------------------------------------------------------------

### Validate Class IDs

Always ensure the class exists before using it.

``` java
if (ClassesCoreAPI.hasClass(classId)) {
    ClassesCoreAPI.selectClass(playerId, classId);
}
```

------------------------------------------------------------------------

### Use API Instead of Internal Services

External plugins should **always use `ClassesCoreAPI`** instead of
directly referencing services.

This ensures:

-   Stability
-   Compatibility
-   Safe initialization handling