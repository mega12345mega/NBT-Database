# <img src="connection/src/main/resources/ui/logo_transparent_plain.svg" height=24 alt="NBT Database Logo" /> NBT Database
[![Release](https://jitpack.io/v/mega12345mega/NBT-Database.svg)](https://jitpack.io/#mega12345mega/NBT-Database)

Store NBT entries in an SQLite database

NBT entries are stored in the format used by the mod [NBT Editor](https://github.com/mega12345mega/NBT-Editor)

[Wiki](https://github.com/mega12345mega/NBT-Database/wiki)

Interfaces:
* GUI
* CLI
* Website (when running a server, read-only)
* Library (`:file`, `:connection`)

You can host servers to allow other people or programs to access databases remotely. See [wiki/Servers](https://github.com/mega12345mega/NBT-Database/wiki/Servers) for details.

**The standard port is 28260, which should be used whenever possible!**

If you only need to work with local `.db` files directly, you can just use the `:file` module.

# Building

```
git clone https://github.com/mega12345mega/NBT-Database.git nbtdatabase
cd nbtdatabase
gradlew build
```

Note that this project uses a LGPL 3 library ([RaphiMC/MinecraftAuth](https://github.com/RaphiMC/MinecraftAuth)) - you can modify this dependency by modifying `ui/build.gradle` and rebuilding.
