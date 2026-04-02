---
title: "Passives"
order: 8
published: true
draft: false
---

# Passives

Passives define **permanent bonuses or modifiers** applied to a class. These effects are automatically active once a class is selected.

Based on the current implementation, passives are used in **two different ways**:

1. **Stat passives** are applied when a player selects a class.
2. **Damage passives** are applied during damage handling when a player attacks an NPC.

---

## Passive Structure

Each passive is defined as an object.

```json
{
  "id": "warrior_health_1",
  "type": "attribute_multiplier",
  "value": 2.0
}
```

Some passive types may require additional fields depending on how your system evolves, but in the current code the most important fields are:

- `id`
- `type`
- `value`

---

## Fields

### ID

**Type:** `string`

The passive ID is the most important routing key in the current implementation.

### Current behavior

When a class is selected, the system checks the passive ID for these keywords:

- `health`
- `stamina`
- `mana`

If the ID contains one of those terms, the passive is mapped to the corresponding stat.

Examples:

```json
"id": "warrior_health_1"
"id": "mage_mana_bonus"
"id": "rogue_stamina_2"
```

If the passive ID does **not** contain one of those keywords, the stat passive is skipped.

### Naming guidance

Because the system currently infers behavior from the ID, passive IDs should follow a predictable naming convention.

Recommended pattern:

```text
<class>_<target>_<level>
```

Examples:

```text
warrior_health_1
mage_mana_2
rogue_stamina_1
ranger_ranged_1
knight_melee_1
```

---

### Type

**Type:** `string`

The passive type determines how the passive is interpreted.

The current PassiveType enum supports the following values:

| Type                   | Description                                     |
|------------------------|-------------------------------------------------|
| `attribute_multiplier` | Multiplies a supported player stat              |
| `attribute_additive`   | Adds a flat value to a supported player stat    |
| `damage_multiplier`    | Multiplies outgoing damage based on attack type |

That means JSON values like these are valid:

```json
"attribute_multiplier"
"attribute_additive"
"damage_multiplier"
```

---

### Value

**Type:** `number`

The value controls the strength of the passive.

How it is interpreted depends on the passive type.

#### For `attribute_multiplier`

The value is applied as a **multiplicative stat modifier**.

Examples:

| Value | Result                    |
|-------|---------------------------|
| `2.0` | Doubles the stat          |
| `1.5` | Increases the stat by 50% |
| `1.0` | No change                 |
| `0.5` | Reduces the stat by half  |

Example:

```json
{
  "id": "warrior_health_1",
  "type": "attribute_multiplier",
  "value": 2.0
}
```

---

#### For `attribute_additive`

The value is applied as a **flat additive modifier**.

Examples:

| Value | Result                    |
|-------|---------------------------|
| `10`  | Adds 10 to the stat       |
| `25`  | Adds 25 to the stat       |
| `-5`  | Subtracts 5 from the stat |

Example:

```json
{
  "id": "mage_mana_1",
  "type": "attribute_additive",
  "value": 25
}
```

---

#### For `damage_multiplier`

The value multiplies outgoing damage when the passive matches the attack type.

Examples:

| Value | Result          |
|-------|-----------------|
| `2.0` | Double damage   |
| `1.5` | 50% more damage |
| `1.0` | No change       |
| `0.5` | Half damage     |

Example:

```json
{
  "id": "ranger_ranged_1",
  "type": "damage_multiplier",
  "value": 1.25
}
```

---

## Supported Passive Types

## `attribute_multiplier`

This passive modifies a supported stat multiplicatively.

### Supported stat targets

In the current implementation, stat targets are inferred from the passive ID and only support:

- `health`
- `stamina`
- `mana`

These are mapped internally to the game's stat indexes.

### Example

```json
{
  "id": "knight_health_1",
  "type": "attribute_multiplier",
  "value": 2.0
}
```

This would apply a multiplicative modifier to the player's health stat when the class is selected.

---

## `attribute_additive`

This passive modifies a supported stat additively.

Like `attribute_multiplier`, the affected stat is inferred from the passive ID.

### Example

```json
{
  "id": "rogue_stamina_1",
  "type": "attribute_additive",
  "value": 15
}
```

This would add a flat stamina bonus when the class is selected.

---

## `damage_multiplier`

This passive modifies outgoing damage.

Unlike stat passives, this one is processed inside the damage system and does not use the stat index map.

### Current matching behavior

The current damage system determines whether an attack is **ranged** or **melee** by examining the damage cause ID.

Ranged attacks are detected if the cause contains terms like:

- `range`
- `ranged`
- `projectile`
- `arrow`
- `bow`
- `crossbow`
- `bolt`
- `shot`
- `bullet`
- `thrown`
- `throw`
- `missile`

If none of those are found, the attack is treated as **melee**.

The passive then checks its own ID:

- If the attack is ranged, the passive ID must contain one of the ranged terms.
- If the attack is melee, the passive ID must contain `melee`.

### Examples

#### Melee passive

```json
{
  "id": "knight_melee_1",
  "type": "damage_multiplier",
  "value": 1.2
}
```

#### Ranged passive

```json
{
  "id": "ranger_ranged_1",
  "type": "damage_multiplier",
  "value": 1.3
}
```

### Stacking

Multiple matching `damage_multiplier` passives stack multiplicatively.

For example:

- passive A = `1.2`
- passive B = `1.5`

Final multiplier:

```text
1.2 * 1.5 = 1.8
```

So the attack would deal 1.8x damage.

---

## Current Limitations

Based on the current code, these limitations exist:

1. **Stat passives only support** `health`, `stamina`, and `mana`.
2. The affected stat is inferred from the **passive ID text**, not from an explicit field.
3. `damage_multiplier` does not use an explicit `damageType` field in the code shown.
4. Damage matching also depends on **passive ID text**, especially `melee` or ranged-related terms.
5. Passives with unmatched IDs are silently skipped.

---

## Recommended Authoring Rules

To avoid passives being ignored, use these rules when writing class data:

### For stat passives

- Include one of:
    - `health`
    - `stamina`
    - `mana`
- Use type:
    - `attribute_multiplier`
    - `attribute_additive`

Examples:

```json
{
  "id": "paladin_health_1",
  "type": "attribute_multiplier",
  "value": 1.5
}
```

```json
{
  "id": "sorcerer_mana_1",
  "type": "attribute_additive",
  "value": 30
}
```

### For damage passives

- Include `melee` for melee bonuses
- Include a ranged-related term such as `ranged`, `bow`, `projectile`, or `arrow` for ranged bonuses
- Use type:
    - `damage_multiplier`

Examples:

```json
{
  "id": "duelist_melee_1",
  "type": "damage_multiplier",
  "value": 1.15
}
```

```json
{
  "id": "hunter_projectile_1",
  "type": "damage_multiplier",
  "value": 1.2
}
```