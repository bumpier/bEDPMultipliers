# bEDPMultipliers

A powerful and flexible multiplier plugin designed to integrate seamlessly with `EdPrison`. It allows for the creation of stacking temporary, permanent, and permission-based multipliers for individual players or globally across the server.

## Features

- **Stacking Multipliers**: All multiplier types are additive, allowing you to combine global events, player-specific bonuses, and rank perks into a single, calculated total.
- **Permanent & Temporary**: Set multipliers that last forever or for a specific duration (e.g., `1d12h30m`).
- **Player & Global Scopes**: Apply multipliers to a single player or to everyone on the server.
- **Permission-Based**: Grant multipliers to players or groups directly through permissions (e.g., `bmultipliers.multi.1.5`).
- **Visual BossBar**: A highly configurable BossBar displays the amount and remaining time for active temporary multipliers.
- **PlaceholderAPI Support**: Integrates with PlaceholderAPI to display all multiplier types anywhere on your server.
- **Robust & Modular**: Built to be efficient and easy to maintain, with clear separation of concerns.

## Dependencies

- **[EdPrison](https://builtbybit.com/resources/edprison-core.24738/)**: This plugin is the core dependency and is required.
- **[PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/)**: (Optional) Required for using the placeholders.

## Commands

| Command                                            | Description                                          | Permission              |
| -------------------------------------------------- | ---------------------------------------------------- | ----------------------- |
| `/bmulti <player>`                                 | Checks a player's total multiplier.                  | `bmultipliers.player`   |
| `/bmulti set <player/global> <amount>`             | Sets a permanent multiplier for a player or globally. | `bmultipliers.admin`    |
| `/bmulti settemp <player/global> <amount> <time>`  | Sets a temporary multiplier.                         | `bmultipliers.admin`    |
| `/bmulti remove <player/global>`                   | Removes multipliers for a player or globally.        | `bmultipliers.admin`    |

## Permissions

- `bmultipliers.player`: Allows the use of the base `/bmulti <player>` command to check multipliers.
- `bmultipliers.admin`: Grants access to all admin subcommands (`set`, `settemp`, `remove`).
- `bmultipliers.multi.<amount>`: Grants a permission-based multiplier. For example, `bmultipliers.multi.1.5` would give a 1.5x multiplier. The plugin will always use the highest value if a player has multiple of these permissions.

## Placeholders

To use these, ensure `PlaceholderAPI` is installed, then run `/papi ecloud download bEDPMultipliers` and `/papi reload`.

- `%bmulti_rankmulti%`: Displays the player's permission-based multiplier.
- `%bmulti_personalmulti%`: Displays the player's permanent multiplier set by command.
- `%bmulti_tempmulti%`: Displays the player's currently active temporary multiplier (personal takes priority over global).
- `%bmulti_totalmulti%`: Displays the player's final combined multiplier from all sources.
