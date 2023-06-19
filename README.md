# Stonecutter
Preprocessor/JCP inspired multi-version environment manager

## About
This gradle/intellij plugin is a manager that allows working on a project that targets multiple compatabile release versions of a dependency. This is mostly meant for Loom-based Fabric projects written in java.

The project is heavily inspired by [Preprocessor](https://github.com/ReplayMod/preprocessor) and [JCP](https://github.com/raydac/java-comment-preprocessor) and the way they work. 
While they are great, I had a few issues with them and so this bad boy was made! Huge credit goes to the developers of these projects for giving me the idea!

For the name Stonecutter, I have another gradle plugin named "`Fletching Table`" and both that plugin and this one are meant to complement Fabric's Loom plugin, you can follow that thinking on your own. 

(This project is experimental, use at your own risk and always back up stuff with vcs such as git)

## How does it work
Stonecutter works by taking a gradle project and splitting it into versioned variants of it, adding those subprojects as the parent's children projects. What this does is allow declaring multiple 
versions of the same project and through the plugin, create multiple versions of the project's code, interweaved using java's comments.

There is the concept of the "active" environment. By convention, inside vcs such as git, it should be the most up-to-date version that the project is targetting and only be switched while working to then be switched back for commits. 
When a version is "active", the code for it exists in the source and the child project for it points its sourcesets onto the active environment's `src` directory. The rest of the code from the other versions is then commented out, as to not interfere with this version's edition of the source.

## Features
### Single build script for all versions
The main `build.gradle` script is used by all of the versions and is provided with information about them. Note that this does not stop the different versions from having their own properties files to simplify the setup.

... (section not finished)

### Versioned code comments
Using semver version constraints that are loaded through Fabric Loader's api from the project's dependencies(you can add it to a dummy configuration to pull it if it's not present already), Stonecutter will check and comment out uneeded code for the active version.

... (section not finished)

### Regex based token remapping
The project can have a defined set of tokens, based on the concepts of regex replace/substitution and capture groups.

... (section not finished)

### Chiseled tasks
Stonecutter is able to set up any task(usually builds or publication tasks), to run concurrently on all versions at once without the need to set the active version for each task.

... (section not finished)

### Intellij IDEA plugin
Stonecutter also contains a complementing IDE plugin for Intellij based software. The plugin is meant to ease the usage of Stonecutter but Stonecutter can definitely work without it.

To install the plugin, [download the same gradle plugin jar from the maven](https://plugins.gradle.org/m2/io/shcm/shsupercm/fabric/Stonecutter/) and add it to IDEA through the plugins screen.
(I am working on making this easier to do)

... (section not finished)

## Setup
The way Stonecutter works, is by applying onto the projects `settings.gradle` to hijack the buildscript of the target multiversion project.

Example:
> `settings.gradle`
```groovy
plugins {
	id 'io.shcm.shsupercm.fabric.stonecutter' version '1.0'
}

// in this example we'll convert :versionedProject into a multi-versioned "stonecutter" project
include ':versionedProject'

stonecutter.create(project(':versionedProject')) {
  // the following definition will add projects ':versionedProject:1.18.2' through to ':versionedProject:1.20'
	versions '1.20', '1.19.4', '1.19.2', '1.18.2'
}
```

Refreshing gradle will then create a new script named `stonecutter.gradle` which is the buildscript of the "controller" versioned project.

The versions of the stonecutter project have their directories in the controller project's `./versions/<VERSION>/`. Those can have their own `gradle.properties` and build/run directories.

[If you need an example, take a look at the test project for Stonecutter.](https://github.com/SHsuperCM/Stonecutter/tree/main/test)

# Changelog
Look at the [commits](https://github.com/SHsuperCM/Stonecutter/commits/main) for a changelog.

## Planned
- [ ] Making a wiki and actually explaining how to operate Stonecutter
- [ ] Add a way to remap the project through the versions using Loom's mappings and Mercury
- [ ] Make the Intellij IDEA plugin easier to install
- [ ] Add more integration features to the Intellij IDEA plugin(switch versions, insert versioned code, create tokens, etc..)
- [x] Proprocessor-style commenting formatter
- [x] Intellij IDEA integration
- [x] Chiseled Tasks
- [x] Regex based token "find and replace" system
