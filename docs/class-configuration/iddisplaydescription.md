---
title: "ID/Display/Description"
order: 5
published: true
draft: false
---

# ID / Display / Description

These fields define the **core identity** of a class definition and how it is referenced throughout the system.

Based on the code you shared, the `classId` is used to retrieve a class from the registry, validate player selection, persist the selected class, and restore restriction behavior.

---

## ID

**Type:** `string`

The class ID is the internal identifier used to look up the class definition.

This means:

- the ID must be unique
- the ID must be stable because it is also saved in the player state

### Authoring guidance

Use IDs that are:

- unique
- lowercase
- consistent
- safe to persist long-term

Recommended format:

```text
<class_name>
<class_name_variant>
```

Examples:

```text
blademaster
arcane_mage
elite_ranger
```

### Important note

Because `classId` is persisted and used for later lookup, changing an existing class ID after release may break saved player data or make previously selected classes unresolvable.

---

## Display Name

**Type:** `string`

The display name is the player-facing class name shown in menus, UI, chat, or documentation.

### Current behavior

The code you shared does not directly use `displayName` during class selection or class clearing. That suggests `displayName` is primarily a presentation field rather than a gameplay key.

### Authoring guidance

Use display names that are:

- readable
- thematic
- suitable for player-facing UI
- allowed to contain spaces and capitalization

Examples:

```text
Blademaster
Arcane Mage
Elite Ranger
```

### Best practice

Keep the display name separate from the internal ID.

Example:

```json
{
  "id": "arcane_mage",
  "displayName": "Arcane Mage"
}
```

This allows you to present clean names to players while preserving a safe internal identifier for code and persistence.

---

## Description

**Type:** `string`

The description is short player-facing text that explains the class fantasy, combat role, or intended playstyle.

### Current behavior

The code you shared does not directly consume `description` during selection, passive application, or restriction loading. This means the description currently acts as a documentation and UI field.

### Authoring guidance

Descriptions should:

- be short and readable
- explain the class theme
- communicate role or playstyle
- help players quickly understand the class

Examples:

```text
A disciplined melee fighter who specializes in sword combat.
A powerful spellcaster who channels mana into devastating attacks.
A mobile ranged specialist who relies on speed and precision.
```

### Best practice

Keep descriptions focused on:

- weapon style
- combat role
- strengths

Avoid over technical implementation details in the player-facing description unless the UI is meant to show system behavior.

---

## Runtime Relationship to Class Selection

From the code shown, these fields serve different purposes:

| Field         | Primary Purpose                                                      |
|---------------|----------------------------------------------------------------------|
| `id`          | Internal lookup, validation, persistence, and restriction assignment |
| `displayName` | Player-facing label                                                  |
| `description` | Player-facing summary text                                           |

This makes `id` the only one of the three that is gameplay-critical in the current implementation.

---

## Recommended Authoring Rules

### For `id`

- it must be unique
- it must not be blank
- it should be lowercase
- it should remain stable after release

### For `displayName`

- it should be human-readable
- it can include spaces and capitalization

### For `description`

- it should be concise
- it should explain the class role or style
- it should be written for players, not systems

---

## Example

```json
{
  "id": "blademaster",
  "displayName": "Blademaster",
  "description": "A melee fighter who specializes in one-handed sword combat."
}
```