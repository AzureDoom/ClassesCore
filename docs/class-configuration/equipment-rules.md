---
title: "Equipment Rules"
order: 9
published: true
draft: false
---

# Equipment Rules

Equipment rules define which weapons and armor a class is allowed to use.

These rules support:

* **exact item IDs**
* **wildcard patterns**
* **TagCore tag references**

---

## Structure

Example structure:

```json
"equipmentRules": {
"allowedWeapons": [
"Weapon_Sword_*",
"Weapon_Dagger_Crude",
"#tagcore:starter_weapons"
],
"allowedArmor": [
"Armor_Adamantite_*",
"Armor_Cloth_*",
"#tagcore:light_armor"
]
}
```

---

## Pattern Types

Equipment rules support three types of entries:

| Type          | Meaning                             | Example                    |
|---------------|-------------------------------------|----------------------------|
| Exact ID      | Matches one specific item ID        | `Weapon_Dagger_Crude`      |
| Wildcard      | Matches item IDs using `*`          | `Weapon_Sword_*`           |
| Tag Reference | Matches items in a TagCore item tag | `#tagcore:starter_weapons` |

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

## Tag Support

Equipment rules support TagCore item tags using the `#` prefix.

Tag entries are useful when items should be grouped by gameplay meaning instead of naming convention.

### Format

```json
"#namespace:tag_name"
```

If no namespace is provided, the default `hytale:` namespace is used.

### Examples

```json
"allowedWeapons": [
  "#tagcore:starter_weapons"
]
```

✔ Allows all items in the `tagcore:starter_weapons` tag.

---

```json
"allowedArmor": [
  "#tagcore:light_armor"
]
```

✔ Allows all armor items in the `tagcore:light_armor` tag.

---

```json
"allowedArmor": [
  "#light_armor"
]
```

✔ Uses the default namespace and resolves as `hytale:light_armor`.

---

### Behavior

* Tag entries match all items in the referenced TagCore item tag
* If the tag does not exist, it is ignored
* If TagCore is unavailable, the tag entry will not match
* Tag checks are performed at runtime through TagCore

---

## Allowed Weapons

**Type:** `array[string]`

Defines which weapon IDs, patterns, or tags a class is allowed to use.

If the array is empty, **all weapons are allowed**.

### Authoring guidance

Use `allowedWeapons` for:

* limiting weapon categories by class
* preventing incompatible loadouts
* preserving balance
* grouping items via wildcard patterns
* grouping items via TagCore tags

Examples:

```json
"allowedWeapons": [
  "Weapon_Sword_*",
  "Weapon_Dagger_Crude",
  "#tagcore:starter_weapons"
]
```

---

## Allowed Armor

**Type:** `array[string]`

Defines which armor IDs, patterns, or tags a class is allowed to equip.

If the array is empty, **all armor is allowed**.

Examples:

```json
"allowedArmor": [
  "Armor_Adamantite_*",
  "Armor_Cloth_*",
  "#tagcore:light_armor"
]
```

---

## Authoring Rules

### Weapon entries

Each weapon entry should:

* be a valid internal item ID, wildcard pattern, or tag reference
* represent an item or group the class should be allowed to equip

### Armor entries

Each armor entry should:

* be a valid internal item ID, wildcard pattern, or tag reference
* represent allowed armor pieces or categories
* use wildcards or tags where appropriate to reduce repetition

---

## Best Practices

* Prefer **TagCore tags for logical groupings**:

  * `#tagcore:starter_weapons`
  * `#tagcore:light_armor`

* Prefer **wildcards for tiers or categories based on naming conventions**:

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

* a class may use tags to allow a curated item group:

  ```json
  "allowedWeapons": ["#tagcore:starter_weapons"]
  ```

* a flexible class may allow everything:

  ```json
  "allowedWeapons": ["*"]
  ```

---

## Summary

* Supports **exact IDs, wildcard patterns, and TagCore tag references**
* Empty arrays = **no restrictions**
* Tags allow item groups to be managed through TagCore
* Wildcards reduce repetition and improve maintainability
* Matching is **ID-based and tag-aware**, not display-name-based
