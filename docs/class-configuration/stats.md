---
title: "Stats"
order: 6
published: true
draft: false
---

# Stats

Stats define **base attributes and level scaling** applied to a class. These values directly modify the player's core stats provided by LevelingCore.

Unlike passives, stats are **numerical values stored directly on the player** and are modified when:
- A class is selected
- The player levels up
- The player levels down

---

## Stat Behavior

Stats are applied in three stages:

### 1. On Class Selection

When a player selects a class:

```
+ base
+ ((level - 1) * perLevel)
```

This means:
- Players receive the base stat immediately
- Players also receive retroactive scaling for their current level

---

### 2. On Level Up

Each level gained applies:

```
+ perLevel
```

---

### 3. On Level Down

Each level lost applies:

```
- perLevel
```

---

## Stat Structure

Each stat is defined as an object inside the `stats` array.

```json
{
  "id": "strength",
  "base": 10,
  "perLevel": 1
}
```

---

## Fields

### ID

**Type:** `string`

The stat identifier must match a supported LevelingCore stat.

### Supported stat IDs

- `strength`
- `agility`
- `perception`
- `vitality`
- `intelligence`
- `constitution`

If an invalid ID is used, the stat will fail to apply.

---

### Base

**Type:** `number`

The base value is applied **once when the class is selected**.

### Behavior

- Added to the player's current stat value
- Does **not overwrite** existing stats
- Safe to apply at any level

### Example

```json
{
  "id": "strength",
  "base": 10,
  "perLevel": 0
}
```

This adds +10 strength when the class is selected.

---

### perLevel

**Type:** `number`

The perLevel value controls how much the stat changes per level.

### Behavior

- Added on level up
- Removed on level down
- Retroactively applied when selecting a class above level 1

### Example

```json
{
  "id": "vitality",
  "base": 10,
  "perLevel": 2
}
```

This results in:
- +10 vitality on class select
- +2 vitality per level

---

## Full Example

```json
"stats": [
  {
    "id": "strength",
    "base": 10,
    "perLevel": 1
  },
  {
    "id": "agility",
    "base": 10,
    "perLevel": 0
  },
  {
    "id": "perception",
    "base": 10,
    "perLevel": 1
  },
  {
    "id": "vitality",
    "base": 10,
    "perLevel": 2
  },
  {
    "id": "intelligence",
    "base": 10,
    "perLevel": 0
  },
  {
    "id": "constitution",
    "base": 10,
    "perLevel": 1
  }
]
```

---

## Stacking Behavior

Stats are **additive** and stack with:

- Existing player stats
- Ability point allocations
- Other system modifications

Example:

```
Base strength from class: +10
Level scaling (level 5, perLevel 1): +4
Manual points: +3

Final strength = current + 17
```

---

## Current Limitations

1. Only the six core stats are supported.
2. Stats must match valid LevelingCore stat IDs.
3. There is no built-in stat cap.
4. Negative values are not recommended and may cause unintended behavior.

---

## Recommended Authoring Rules

### General Rules

- Always use valid stat IDs
- Keep base values reasonable
- Use perLevel for scaling, not base inflation

---

### Good Example

```json
{
  "id": "vitality",
  "base": 12,
  "perLevel": 2
}
```

---

### Avoid

```json
{
  "id": "strength",
  "base": 1000,
  "perLevel": 0
}
```

This creates poor scaling and balance issues.

---

## Design Notes

- Stats are **applied directly to stored values**, not as modifiers
- This makes them compatible with:
    - manual stat allocation
    - other systems modifying stats
- Level scaling is handled incrementally to avoid recalculation issues

---

## Future Expansion

This structure supports adding fields like:

```json
{
  "id": "strength",
  "base": 10,
  "perLevel": 1,
  "cap": 100,
  "scaling": "linear"
}
```

These are not currently implemented but can be added later without breaking compatibility.