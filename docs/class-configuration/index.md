---
title: "Class Configuration"
order: 5
published: true
draft: false
---

# Full Class Definition Example

The following example shows a complete class definition.

It combines all documented sections:
- [Class identity fields](https://wiki.hytalemodding.dev/mod/classescore/iddisplaydescription)
- [Stats](https://wiki.hytalemodding.dev/mod/classescore/stats)
- [Passive modifiers](https://wiki.hytalemodding.dev/mod/classescore/passives)
- [Equipment restrictions](https://wiki.hytalemodding.dev/mod/classescore/equipment-rules)

Each field is explained inline.

### Example
```json
{
  "schemaVersion": 1,
  "id": "blademaster", // Unique internal identifier used by the class registry.
  "displayName": "Blademaster", // Player-facing name shown in UI, menus, and documentation.
  "description": "They use one medium-sized one-handed blade in battle.", // Short summary describing the class fantasy and combat style.

  "stats": [
    {
      "id": "strength", // The stat identifier. Must match a valid LevelingCore stat (strength, agility, perception, vitality, intelligence, constitution).
      "base": 10, // Flat amount added to the player's stat when the class is selected.
      "perLevel": 1 // Amount automatically gained per level (and removed on level down).
    },
    {
      "id": "agility", // Agility stat identifier.
      "base": 10, // Base bonus applied on class selection.
      "perLevel": 0 // No automatic scaling per level for this stat.
    },
    {
      "id": "perception",
      "base": 10,
      "perLevel": 1
    },
    {
      "id": "vitality",
      "base": 10,
      "perLevel": 2 // Gains 2 vitality per level.
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
  ],

  // IMPORTANT:
  // - Base stats are applied once when the class is selected.
  // - If the player is already above level 1, they also receive retroactive perLevel scaling.
  // - Future level-ups and level-downs automatically adjust stats using perLevel.

  "passives": [
    {
      "id": "blademaster_health_1", // Unique identifier for this passive effect.
      "type": "attribute_multiplier", // Passive type that multiplies a player stat.
      "attribute": "max_health", // The attribute affected by this passive.
      "value": 2 // Multiplier applied to the attribute (2 = double health).
    },
    {
      "id": "blademaster_melee_1", // Identifier for a damage-related passive.
      "type": "damage_multiplier", // Passive type that modifies outgoing damage.
      "damageType": "melee", // Damage category affected by this modifier.
      "value": 1.5 // Multiplier applied to melee damage (1.5 = 50% more damage).
    }
  ],

  "equipmentRules": {
    "allowedWeapons": [
      "Weapon_Sword_Adamantite", // Weapon ID the class is allowed to use.
      "Weapon_Sword_Crude", // Additional allowed weapon ID.
      "Weapon_Sword_*" // Supports Wildcard usage.
    ],
    "allowedArmor": [
      "Armor_Adamantite_Chest", // Chest armor piece allowed for this class.
      "Armor_Adamantite_Legs", // Leg armor piece allowed for this class.
      "Armor_Adamantite_Head", // Head armor piece allowed for this class.
      "Armor_Adamantite_Hands", // Hand armor piece allowed for this class.
      "Armor_Adamantite_*", // Supports Wildcard usage.
    ]
  }
}
```