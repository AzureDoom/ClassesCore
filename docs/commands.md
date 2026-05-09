---
title: "Commands"
order: 4
published: true
draft: false
---

# `/class`

Opens the class menu as long as the player is not in a class.

### Usage
`/class`

### Permission
`com.azuredoom.classescore.command.classescore.class`

# `/class join <class>`

Allows the player to join a class if they have the required permissions.

### Required Arguments
`class` - The class to join.

### Usage
`/class join <class>`

# `/class leave`

### Permission
`com.azuredoom.classescore.command.classescore.joinclass`

Allows the player to leave their current class if they have the required permissions.

### Usage
`/class leave`
`/class leave --player=PlayerName`

### Optional Arguments
`--player=PlayerName` - The player to leave the class for. If not specified, the command will be executed for the sender.

### Permission
`com.azuredoom.classescore.command.classescore.leaveclass`

# `/class list`

Lists all available classes and their information.

### Usage
`/class list`

### Permission
`com.azuredoom.classescore.command.classescore.listclasses`

# `/class reload`

Reloads classes from all sources.

### Usage
`/class reload`

### Permission
`com.azuredoom.classescore.command.classescore.reloadclasses`