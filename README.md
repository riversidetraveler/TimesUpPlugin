# TimesUpPlugin
TimesUpPlugin is a console plugin intended to help parents to enforce a limit on the amount of time their kids spend in a Minecraft session. This plugin admittedly is a very focused solution to a specific problem I needed to address as a dad. It takes me out of the negotiation loop and puts the server in charge of "deciding" when its time to stop playing Minecraft. This approach is working for me but YMMV.

This plugin is written against the Bukkit API and is intendend for use only on Minecraft servers that support that API. I've persionally tested using [SpigotMC](https://www.spigotmc.org/). 

## Building

The project uses a standard [Apache Maven](http://maven.apache.org/) build script. Running mvn clean package will produce a JAR file named TimesUpPlugin-1.0-SNAPSHOT.jar. 

## Deploying

Copy the TimesUpPlugin-1.0-SNAPSHOT.jar file into your Minecraft server's plugins directory. If you're using SpigotMC the path would be:

```
<SpigotHome>
            |
             --> plugins
```
## Running
When you start your Minecraft server watch for the TimesUpPlugin as the server goes through its startup process:
```
[17:10:06 INFO]: Vine Growth Modifier: 100%
[17:10:06 INFO]: Cocoa Growth Modifier: 100%
[17:10:06 INFO]: Entity Activation Range: An 32 / Mo 32 / Mi 16
[17:10:06 INFO]: Entity Tracking Range: Pl 48 / An 48 / Mo 48 / Mi 32 / Other 64
[17:10:06 INFO]: Hopper Transfer: 8 Hopper Check: 1 Hopper Amount: 1
[17:10:06 INFO]: Mob Spawn Range: 4
[17:10:06 INFO]: Preparing start region for level 0 (Seed: 5726035235674885236)
[17:10:07 INFO]: Preparing spawn area: 48%
[17:10:07 INFO]: Preparing start region for level 1 (Seed: 5726035235674885236)
[17:10:08 INFO]: Preparing start region for level 2 (Seed: 5726035235674885236)
[17:10:08 INFO]: [TimesUpPlugin] Enabling TimesUpPlugin v1.0
```

## Use
TimesUpPlugin supports 5 commands. Gameplay timers must be started from the console. The command is structured as follows:
```
timesup set 30
```
The gameplay limit is expressed in minutes.

Once the limit is set, the plugin will emit a message to all players every 5 minutes indicating how much game play time remains.

In the event that a player needs to pause the timer the following command can be issued from either the console OR the client:

```
timesup pause
```

The expended time will be recorded and the current timer canceled. To resume the timer issue the following command:

```
timesup resume
```

The resume command will resume the timer taking into account how much time was already spent playing.

If the timer should be cancelled outright, the following command can be issued from the console:

```
timesup cancel
```

Also, at any time a player OR the console can issue the following command to determine how much time is left:

```
timesup timeleft
```

# Future Improvements
It's clear that exposing the pause/resume capability to players opens the system to abuse. In a future release I plan on adding tracking of pause requests AND limiting the request period to 5 minutes per request.


