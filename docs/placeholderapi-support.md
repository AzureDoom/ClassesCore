---
title: "PlaceholderAPI Support"
order: 12
published: true
draft: false
---

ClassesCore includes native support for PlaceholderAPI, allowing you to display player class data dynamically in chat messages, UI text, scoreboards, and more.

## Placeholder Format

All placeholders provided by ClassesCore follow this format:

```
%classescore_variable%
```

## Supported Placeholders

| Placeholder                | Description                          |
|----------------------------|--------------------------------------|
| `%classescore_class%`      | Player’s selected class ID           |
| `%classescore_class_id%`   | Player’s selected class ID           |
| `%classescore_class_name%` | Player’s selected class display name |

## Usage Examples

### Basic Chat Message
```
player.sendMessage(
    PlaceholderAPI.setPlaceholders(
        player.getPlayerRef(),
        Message.raw("Your class is %classescore_class_name%.")
    )
);
```

## Notes

- Placeholders are resolved per-player
- Placeholder names are case-insensitive
- You must pass a valid `PlayerRef` to `PlaceholderAPI.setPlaceholders(...)`