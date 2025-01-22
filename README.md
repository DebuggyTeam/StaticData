# StaticData

Static data can be supplied and consumed in several ways. We will start with the supply end.

## In a mod

For a mod set up in the typical fashion, the staticdata root is at `src/main/resources/staticdata/`

Folders inside this directory correspond to the namespace *of the mod that consumes the data*. So, for example, if you're supplying data to `architecture_extensions`, the data will go in `src/main/resources/staticdata/architecture_extensions/`. Any json in this directory following the archEx schema will be automatically picked up and used. All data will automatically be "credited" to the mod that supplied it.


## As flat per-instance staticdata

There is also a "staticdata" folder inside the minecraft folder. It is lazily created - that is, if it doesn't exist, and a request for staticdata is made, the folder will also be created to help you see where to supply the data. You can also proactively create this folder and put data there. This follows the same rules, e.g. `.minecraft/staticdata/architecture_extensions` can contain the same json as the example above. All data shipped this way will be shipped under the special namespace "file".


## As a staticdata pack

Either of these staticdata roots may contain zip files (yes, that means you can embed a staticdata pack inside your mod). The root folder inside this zip should be called "staticdata", and that is its own staticdata root. This would allow you to ship a file that was simultaneously a respack and a datapack and a staticdata pack, but mojang split the res and data versions, so we can't. Blame mojang. Keeping with our example, it would be `example.zip > /staticdata/architecture_extensions/`


## Load Order / Listing Order

Staticdata consumers will *always* see staticdata packs supplied by mods, then raw staticdata supplied by mods, then staticdata packs, then raw files in the minecraft/staticdata folder. Since mod data is listed in the order that the mod loader reports, and at least in the case of fabric, this is dependency resolution order (!!!), declaring dependencies will cause your mod's staticdata to appear after the data of the mod you depend on.


Data consumers should keep in mind that the mods supplying the data likely *have not yet had a chance to register their blocks and items*, so any game objects referenced from static data quite likely don't exist yet. It is recommended to use registry listeners to resolve these objects as they come in instead of trying to immediately resolve everything when you do your staticdata scan in your mod initializer.


## How to consume staticdata

The most popular way to consume data is to scoop up a collection of json files in a designated folder. For example:

```java
Gson gson = new GsonBuilder().create();

// We're asking for *all* data in staticdata/mymod/blocks/ and any subdirectories
for(StaticDataItem dataItem : StaticData.getDataInDirectory(Identifier.of("mymod", "blocks"), true)) {
    try {
        MyBlockConfig config = gson.fromJson(item.getAsString(), MyBlockConfig.class);
        // do something with config
    } catch (IOException ex) {
        LOGGER.warn("There was a problem with staticdata file \""+item.getResourceId()+"\" supplied by "+item.getModId());
    }
}
```

We use getDataInDirectory to create a list of data items, then go through each one and try to load it. If something goes wrong, we report both the file and the supplier, since both are needed to track down the problem. Notice that mods can't lie about where their data comes from!


If you only ever consume one file, you can ask for it specifically:

```java
Gson gson = new GsonBuilder().create();

// We're asking for all copies of staticdata/mymod/block_config.json - note that even though we're asking for
// a specific file, there may be multiple suppliers!
for(StaticDataItem dataItem : StaticData.getExactData(Identifier.of("mymod", "config.json"), true)) {
    try {
        MyConfig config = gson.fromJson(item.getAsString(), MyConfig.class);
        // do something with config
    } catch (IOException ex) {
        LOGGER.warn("There was a problem with staticdata file \""+item.getResourceId()+"\" supplied by "+item.getModId());
    }
}
```

You can include directories in this as normal with Identifiers.
