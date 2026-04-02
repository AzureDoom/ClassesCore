---
title: "Equipment Rules"
order: 9
published: true
draft: false
---

# Equipment Rules

Equipment rules define which weapons and armor a class is allowed to use.

These rules support both **exact item IDs** and **wildcard patterns** for flexible configuration.

---

## Structure

Example structure:

```json
"equipmentRules": {
  "allowedWeapons": [
    "Weapon_Sword_*",
    "Weapon_Dagger_Crude"
  ],
  "allowedArmor": [
    "Armor_Adamantite_*",
    "Armor_Cloth_*"
  ]
}
```

---

## Wildcard Support

Equipment rules support simple wildcard matching using `*`.

### Supported patterns

| Pattern    | Meaning                               | Example          |
|------------|---------------------------------------|------------------|
| `*`        | Matches everything                    | `*`              |
| `prefix*`  | Matches anything starting with prefix | `Weapon_Sword_*` |
| `*suffix`  | Matches anything ending with suffix   | `*_Crude`        |
| `*middle*` | Matches anything containing text      | `*Adamantite*`   |

### Examples

```json
"allowedWeapons": [
  "Weapon_Sword_*"
]
```

✔ Allows:

* `Weapon_Sword_Crude`
* `Weapon_Sword_Adamantite`

❌ Does NOT allow:

* `Weapon_Axe_Crude`

---

```json
"allowedArmor": [
  "Armor_Adamantite_*"
]
```

✔ Allows all Adamantite armor pieces without listing each individually.

---

```json
"allowedWeapons": [
  "*"
]
```

✔ Allows all weapons.

---

## Allowed Weapons

**Type:** `array[string]`

Defines which weapon IDs or patterns a class is allowed to use.

If the array is empty, **all weapons are allowed**.

### Authoring guidance

Use `allowedWeapons` for:

* limiting weapon categories by class
* preventing incompatible loadouts
* preserving balance
* grouping items via wildcard patterns

Examples:

```json
"allowedWeapons": [
  "Weapon_Sword_*",
  "Weapon_Dagger_Crude"
]
```

---

## Allowed Armor

**Type:** `array[string]`

Defines which armor IDs or patterns a class is allowed to equip.

If the array is empty, **all armor is allowed**.

Examples:

```json
"allowedArmor": [
  "Armor_Adamantite_*",
  "Armor_Cloth_*"
]
```

---

## Authoring Rules

### Weapon entries

Each weapon entry should:

* be a valid internal item ID **or wildcard pattern**
* represent an item or group the class should be allowed to equip

### Armor entries

Each armor entry should:

* be a valid internal item ID **or wildcard pattern**
* represent allowed armor pieces or categories
* use wildcards where appropriate to reduce repetition

---

## Best Practices

* Prefer **wildcards for tiers or categories**:

    * `"Weapon_Sword_*"` instead of listing every sword
* Use **exact IDs for exceptions or special items**
* Keep rules **clear and intentional**
* Avoid overly broad patterns unless intended (e.g., `"*"`)

---

## Design Considerations

When writing equipment rules, consider:

* combat role
* balance
* progression
* whether restrictions are broad or highly specific

Examples:

* a sword-focused class may use:

  ```json
  "allowedWeapons": ["Weapon_Sword_*"]
  ```
* a mage may restrict armor:

  ```json
  "allowedArmor": ["Armor_Cloth_*"]
  ```
* a flexible class may allow everything:

  ```json
  "allowedWeapons": ["*"]
  ```

---

## Summary

* Supports **exact IDs and wildcard patterns**
* Empty arrays = **no restrictions**
* Wildcards reduce repetition and improve maintainability
* Matching is **pattern-based, not display-name-based**