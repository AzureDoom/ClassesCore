![Header](https://www.bisecthosting.com/images/CF/CLASSES_CORE/MP_CLASSESCORE_Header.webp)

![Description](https://www.bisecthosting.com/images/CF/CLASSES_CORE/MP_CLASSESCORE_Description.webp)

ClassesCore is a flexible and developer-friendly foundation mod that provides a complete framework for implementing class-based gameplay systems. It allows server creators and developers to define player roles, enforce restrictions, and build scalable progression systems through clean APIs and integrations.

ClassesCore is designed to work alongside other mods, particularly **LevelingCore**, which handles progression mechanics such as XP and leveling, while ClassesCore focuses on defining and enforcing class identity.

***

## Features

### Modular Class System

*   Register and manage custom player classes
*   Flexible architecture for extending and modifying class behavior
*   Clean separation between class logic and progression systems

### Player Stats & Attributes

*   Built-in stat system for handling attributes like damage and scaling
*   Easily extendable for custom stat mechanics

### Damage & Combat Integration

*   Hooks into combat systems for class-based damage handling
*   Supports scaling damage based on class stats
*   Designed for compatibility with external combat mods

### Item & Armor Restrictions

*   Restrict items and armor based on player class
*   Prevent misuse of gear outside intended roles
*   Fully configurable and extendable

### Player Class Management

*   Persistent player class storage via H2, MySQL, MariaDB, and PostgreSQL
*   Handles join/leave events and class switching

### Commands

*   `/class` — Opens class selection screen if players does not have class
*   `/class join <class>` — Assign a class to a player
*   `/class leave` — Remove current class

### Lightweight & Optimized

*   Designed for minimal performance overhead
*   Efficient event-driven architecture
*   Suitable for large servers

***

## Configuration

ClassesCore provides flexible configuration for:

*   Class definitions
*   Item restrictions
*   Stat scaling and behavior
*   Integration hooks

Full documentation: [https://wiki.hytalemodding.dev/mod/classescore](https://wiki.hytalemodding.dev/mod/classescore)

***

## Compatibility

*   Designed to integrate with:
    *   LevelingCore (required)
    *   PlaceholderAPI (optional)
    *   DynamicTooltipsLib (optional)

***

## For Developers

ClassesCore exposes a powerful API for all features.

Developer documentation: [https://wiki.hytalemodding.dev/mod/classescore/developer-api](https://wiki.hytalemodding.dev/mod/classescore/developer-api)

```
ClassesCoreAPI.getClassServiceIfPresent().ifPresent(service -> {
    // interact with Class Service
});
```

```
ClassesCoreAPI.getClassRegistryIfPresent().ifPresent(registry -> {
    // interact with Class Registry 
});
```

***

## Issues & Feedback

Found a bug or have a suggestion?  
Report it here: [https://github.com/AzureDoom/ClassesCore/issues](https://github.com/AzureDoom/ClassesCore/issues)

***

### Hosting Partner

Looking for a reliable server to run **ClassesCore** and other Hytale mods?

**BisectHosting** offers pre-configured game servers, fast setup, and solid performance for modded environments.

Use code **azuredoom** for **25% off your first month**.

[![BisectHosting](https://www.bisecthosting.com/images/CF/CLASSES_CORE/MP_CLASSESCORE_Promo.webp)](https://url-shortener.curseforge.com/z2g8c)