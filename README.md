# <img src="connection/src/main/resources/logo_transparent_plain.svg" height=24 alt="NBT Database Logo" /> NBT Database
[![Release](https://jitpack.io/v/mega12345mega/NBT-Database.svg)](https://jitpack.io/#mega12345mega/NBT-Database)

Store NBT entries in an SQLite database

NBT entries are stored in the format used by the mod [NBT Editor](https://github.com/mega12345mega/NBT-Editor)

[Wiki](https://github.com/mega12345mega/NBT-Database/wiki)

Interfaces:
* GUI
* CLI
* Website (when running a server, read-only)
* Library (`:file`, `:connection`)

An option in the GUI, command in the CLI, and `:connection` module allow you to host a NBT Database server.
You can then connect to the server with all of those, and additionally, you can put the server address into a browser.
Note that WebSockets are also supported, but you would have to implement the protocol yourself.

**The standard port is 28260, which should be used whenever possible!**

If you only need to work with local `.db` files directly, you can just use the `:file` module.

# Building

```
git clone https://github.com/mega12345mega/NBT-Database.git nbtdatabase
cd nbtdatabase
gradlew build
```

Note that this project uses a LGPL 3 library ([RaphiMC/MinecraftAuth](https://github.com/RaphiMC/MinecraftAuth)) - you can modify this dependency by modifying `connection/build.gradle` and rebuilding.