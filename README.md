# OdooLS for PyCharm

<!-- Plugin description -->
**Language server support for Odoo projects in PyCharm.**

This PyCharm plugin integrates the OdooLS language server for Odoo projects.

It provides advanced language features, including:

- Hover information

- Go to definition

- Autocompletion

- Diagnostics

For installation instructions and configuration details, see the [OdooLS](https://github.com/odoo/odoo-ls)
<!-- Plugin description end -->

## Installation

### Warning: OdooLS for PyCharm is only available in "unified version" of PyCharm. If you use the Community Edition, you have to update your program to the unified version (it's free and will be automatic starting from 2025.3). See https://blog.jetbrains.com/pycharm/2025/04/unified-pycharm/

- Using the IDE built-in plugin system (Soon):

  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "OdooLS-Pycharm"</kbd> >
  <kbd>Install</kbd>

- Using JetBrains Marketplace (Soon):

  Go to [JetBrains Marketplace](https://plugins.jetbrains.com/plugin) and install it by clicking the <kbd>Install to ...</kbd> button in case your IDE is running.

  You can also download the [latest release](https://plugins.jetbrains.com/plugin) from JetBrains Marketplace and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

- Manually:

  Download the [latest release](https://github.com/odoo/odoo-ls-pycharm/releases) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

## Build the plugin manually

To build the plugin, clone this repository and its submodule with

```shell
git clone git@github.com:odoo/odoo-ls-pycharm.git
cd odoo-ls-pycharm
git submodule init
git submodule update
```

Then, you have to put OdooLs binaries (and .pdb for windows) in src/main/resources/odoo-binaries.
Then, you can launch

```shell
./gradlew clean buildPlugin
```

Your plugin will be in build/distributions


---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
