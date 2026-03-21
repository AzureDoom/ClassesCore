---
title: "Creating a Class"
order: 9
published: true
draft: false
---

# ClassesCore – Class JSON Configuration

This guide explains how to create a **class configuration JSON file** used by **ClassesCore**.

Classes define gameplay roles for players and can restrict equipment such as weapons and armor.

---

## JSON Generator (Recommended)

To simplify creating class files, you can use the official generator: https://azuredoom.com/classescore/

The generator allows you to:

* build class JSON visually
* avoid syntax errors
* quickly configure stats, passives, and equipment rules
* export ready-to-use JSON files
* generate asset packs

**Recommended workflow:**

1. Create or edit your class using the generator
2. Export the JSON (or pack)
3. Place it into your `classes/` folder
4. Start the server and test

---

## Where Files Are Loaded From

Class configuration files are loaded from either:

### Mod resources

```
resources/classes/
```

### Asset zip packs

```
classes/
```

When the game loads, ClassesCore automatically scans these folders and registers all class definitions.

---

## File Structure

Example project structure:

```
my-mod/
├─ resources/
│  └─ classes/
│     ├─ warrior.json
│     ├─ mage.json
│     └─ archer.json
```

Or inside an asset zip:

```
assets.zip
└─ classes/
   └─ warrior.json
```

Each file represents **one class definition**.

---

## Testing a Class

After creating your JSON file:

1. Place it inside the **classes folder**
2. Start the server
3. Use the `/joinclass` command to select the class

---

## Best Practices

Do **not** store multiple classes in one file.

Correct:

```
classes/
  warrior.json
  mage.json
  archer.json
```