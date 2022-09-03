# Velope

Simple Velocity plugin for server balancing & organising.

## Features

- Server groups with balancer and commands to have direct access to them
- Kick handler
- Lobby command (go to one of the current server's parent group)
- List of veloped servers, smart config reload (WIP)
- Initial sever group (instead of just one static server, players will be connected to the group's balancer determined
  server on join)
- [AdvancedPortals](https://www.spigotmc.org/resources/advanced-portals.14356/) integration.

### AdvancedPortals integration

If you have identically named destinations on all the group's servers you can specify its name for the
portal (bungee:<group_name>), and it will work on any server (inside the group). Be sure to enable it:
```json
  "integrations": {
    "advancedPortalsSupportEnabled": true
  },
...
```

## Commands

- /velope - Get info about plugin as well as get a list of accessible commands | (no permission)
- /velope list - Get list of all veloped servers (groups) there are | velope.list
- /velope reload - Reload config | velope.reload
- /velope recent <player_name> - View the most recent redirect from Velope | velope.recent
- /vstatus <server_name> - Get status of either regular or veloped server | velope.status.use
- /lobby (/leave, /back) - Connect to parent veloped Server (if there's any) | velope.use.lobby

## Config (Description)

Example configuration:

```json
{
  "integrations": {
    "advancedPortalsSupportEnabled": false
  },
  "groups": [
    {
      "name": "hubs",
      "servers": [
        "hub0"
      ],
      "balanceStrategy": "HIGHEST",
      "command": {
        "label": "hub",
        "aliases": [
          "root"
        ]
      }
    },
    {
      "name": "bedwars_lobbies",
      "servers": [
        "bwlobby0"
      ],
      "balanceStrategy": "LOWEST",
      "parent": "hubs",
      "command": {
        "label": "bedwars",
        "aliases": [
          "bw"
        ]
      }
    }
  ],
  "rootGroup": "hubs",
  "initialGroup": "hubs",
  "fetchOnlineAlternativeEnabled": false,
  "pingerSettings": {
    "cacheTtl": 10000,
    "pingInterval": 10000,
    "logUnavailableCooldown": 120000
  }
}
```

Here you define server groups. Each server group will be represented with its own Velocity server, which I call "
Veloped" server.

Name of Veloped server is equal to the name of the group. When the player connects to one of those servers,
Velope redirects them to the server chosen by the balancer amongst those defined in a corresponding group.
You can specify commands to access each individual group as well as its aliases and a permission to use that command.

Root group is a group that will be used when there are no servers in the parent group (right now, only KickHandler
utilizes it).

Initial group is used when player initially connects to the proxy: instead of just one static server, player will be
connected to the group's balancer determined server.

In pinger settings section you can change:

- Cache TTL - how long server information should be cached until it will be refreshed
- Ping Interval - specifies the time interval servers are pinged to retrieve their info
- Log Unavailable Cooldown - specifies the cooldown for logging unsuccessful pings

You can specify whether Velope should grab online from:

- `"fetchOnlineAlternativeEnabled": false` from ping (might include fake players)
- `"fetchOnlineAlternativeEnabled": true` from Velocity API

### Balancer Strategies

For the moment there are two basic balancer strategies:

- FIRST - created for testing purposes, it always outputs the first server that has been associated with a group
- HIGHEST - basically tries to fill the server: outputs server with the largest amount of players connected
- LOWEST - outputs server with the least amount of players connected'
- RANDOM - outputs random available server

## Support

This plugin is somewhat just a bit better than "proof-of-concept" so there's a lot of things to hone.

Contact me if you face any issues either directly

Discord: Crying Lightning#4888   
Or by joining my cosy [Discord Server](https://theseems.ru/coffeehub/discord)

## Thanks
Lucifer_ (AdvancedPortals), cHRIS (Balancing) - for suggesting ideas & testing