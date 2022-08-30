# Velope

Simple Velocity (only) plugin for basic server balancing

## Features

- Server groups with balancer and commands to have direct access to them
- Kick handler
- Lobby command (go to one of the current server's parent group)
- List of veloped servers, ability to reload plugin (WIP)

## Commands

- /velope - Get info about plugin as well as get a list of accessible commands | (no permission)
- /velope list - Get list of all veloped servers (groups) there are | velope.list
- /vstatus <server_name> - Get status of either regular or veloped server | velope.status
- /lobby (/leave, /back) - Connect to parent veloped Server (if there's any)

## Config (Description)

Example configuration:

```json
{
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
      "balanceStrategy": "HIGHEST",
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
  "initialGroup": "hubs"
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

### Balancer Strategies

For the moment there are two basic balancer strategies:

- FIRST - created for testing purposes, it always outputs the first server that has been associated with a group
- HIGHEST - basically tries to fill the server: outputs server with largest amount of players connected

## Support
This plugin is somewhat just a bit better than "proof-of-concept" so there's a lot of things to hone.  

Contact me if you face any issues either directly  

Discord: Crying Lightning#4888   
Or join my cosy (empty) [Discord Server](https://discord.gg/AucwzPdQDh)