---
title: "Configuration"
order: 3
published: true
draft: false
---

# Configuration File Location

ClassesCore’s configuration file is stored in the following location: `/mods/com.azuredoom_classescore/classescore.json`

### Important Notes

- ClassesCore will only read configuration values from `classescore.json` in the path above.
- Always stop the server before editing the configuration file to avoid data loss or partial writes.

## `JDBC_Connection` (String)

- **Type:** String
- **Description:**  
  JDBC connection string for the database used to store plugin data.  
  Supports **H2**, **MySQL**, **MariaDB**, and **PostgreSQL**.

  External databases require a pre-existing database and valid credentials.  
  Credentials may be provided via dedicated configuration fields or JDBC URL query parameters.
- **Default:** H2 file database in the plugin data directory.

The `JDBC_Connection` configuration controls how ClassesCore stores player data such as UUID and experience.

### Supported JDBC URLs

| Database       | Example JDBC URLs                                                                |
|----------------|----------------------------------------------------------------------------------|
| **H2 (file)**  | `jdbc:h2:file:./mods/com.azuredoom_classescore/data/classescore;MODE=PostgreSQL` |
| **MySQL**      | `jdbc:mysql://host:port/dbname?user=dbuser&password=dbpass`                      |
| **MariaDB**    | `jdbc:mariadb://host:port/dbname?user=dbuser&password=dbpass`                    |
| **PostgreSQL** | `jdbc:postgresql://host:port/dbname?user=dbuser&password=dbpass`                 |

**Notes**
- H2 typically uses an empty username and password unless explicitly configured.
- MySQL, MariaDB, and PostgreSQL require valid credentials.

## `Enable_Class_Item_Restrictions` (Boolean)
**Default:** `true`

When enabled, the [Equipment Rules](https://wiki.hytalemodding.dev/mod/classescore/equipment-rules) system is used.

## `Enable_Class_Selection_UI_On_Join` (Boolean)
**Default:** `true`

When enabled, a UI is shown to players upon joining the server to select their class if they have not yet selected one.