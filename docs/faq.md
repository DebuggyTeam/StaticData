# Frequently Asked Questions

These are not actually frequently asked yet, but they're important to know, so don't say I didn't warn you.

## Why?

As an enthusiastic user and writer of cosmetic mods, it irks me that there's no good way to conditionally register a block. This is a way to let mods seamlessly share info to each other, and do so early enough to make those key block registration decisions. One mod offers a tree, another mod makes all sorts of cool things out of the wood. Everyone wins.

## Is there a fabric version?

Only for older minecraft versions up to 1.14.4, here - https://github.com/CottonMC/StaticData

Static Data is now a Quilt exclusive. The reasons for this are complex, and have to do with the kind of community custodianship I want to encourage, but the end result is that I will not be doing backports to fabric.

## Is there a forge version?

There never has been, and never will be. I would respectfully ask that you do not port this mod to forge. It is not a safe or welcoming community and I do not want to feed the trolls.

## Can fabric mods supply static data?

Yes. Even though modern Static Data does not run on Fabric, it will see and load static data from fabric mods when they are running in quilt.

## How early is early?

Extremely early! Static data is guaranteed to be valid the moment QuiltLoader, QSL, or QFAPI calls its first entrypoint. Before that, the library may function, but due to the vaguaries of modloaders and classpaths and URLClassLoader, I can't make any guarantees on how it will function and what data you'll find.

## Why isn't there dynamic, static data?

Really it has to do with where the data fences **must** go in order to make the guarantees that static data makes, and the consequences of those data fences.

First, you need to be able to make registry decisions in response to static data. That means the data needs to be "frozen" before the very first Block, Entity, Tag, or ColorProvider is registered. It is completely valid to make these registrations from any entrypoint (with the exception that client-side registrations must happen from a clientInitializer or other client-side entrypoint). So staticdata must be "frozen" before the first entrypoint.

In order to run staticdata registration code before the first entrypoint, we'd need a mixin or other strategy to create a new "early init" phase of startup. That itself isn't a big deal. We'd also need staticdata to manually inspect jars to find staticdata hooks, creating instances of mod classes which *also* isn't a big deal.

But there's a reason we don't have an "early init" phase already. Mods aren't really fully constructed until init happens. It's not just about you and your init - if you're careful, you could probably get away with it. But what if your config library's not ready yet? There are *really subtle* opportunities for bugs here. If you're not touching registries, or you're touching registries with datapack support, consider using data instead for these more dynamic cases.
