# medic-collect

## Overview

These instructions should help you get setup using Eclipse to develop on Medic Collect, building JavaRosa from source.

Please update these notes as needed. You will also find instructions on preparing builds for deployments.

## Instructions

### Command line

To build and deploy the APK to a connected device/emulator:

	make

### Eclipse/Android Studio

In Eclipse `Import project`

In project.properties change `target=Google Inc.:Google APIs:19` to `target=android-19`

Add the following to libs:
- google-play-services.jar
- libphonenumber-7.0.2.jar
- maps.jar

Remove '../play-services' from `Project Settings` > `Android` > `Library`

Set Java Compiler: JDK Compliance 1.7

Import existing projects from path to: javarosa

import only:
- javarosa-core
- javarosa-libs
- (not needed) schema-generator
- (not needed) validator-desktop-gui

Will be "missing" required libraries, which are available as a rar on javarosa's website.
Copy to javarosa-libs:
- j2meunit-javarosa.jar
- kxml2-2.3.0.jar

- (not needed unless imported validator-desktop-gui) regexp-me.jar

(not needed unless imported schema-generator)
Copy to schema-generator/lib:
- xpp3-1.1.4.jar

Go to `javarosa-core` > `properties` > `Java Build Path` > `Libraries`
correct the missing jar paths by adding them from javarosa-libs

Go to the `Order and Export tab`
move the new jars to match order and checkmark of the jars they replace

Go back to `Libraries`
Remove the 2 missing (and now replaced) jars

Go to `opendatakit.collect` > `properties` > `Java Build Path` > `Projects`
`Add...` then select `javarosa-core`
Go to the `Order and Export` tab and move `javarosa-core` above `Android Private Libraries` and check it

Remove `javarosa-libraries-2015-01-10.jar` from opendatakit.collect/libs

## Project-Specific Builds for Deployments

In order to deploy projects using Medic Collect you need to install and configure Medic Collect on every user's phone, as well as have a running [Medic Mobile instance](https://github.com/medic/medic-webapp/) to receive the data. Some features were added to Medic Collect specifically to make it simpler to deploy on a large number of phones in a minimum amount of time. Follow the steps below to avoid manually having to configure the forms and settings on each phone.

### Forms
Forms can be added manually to any Medic Collect by adding the XForm XML files in /medicmobile/forms on the SD card. If the forms are unlikely to change and you need to deploy Medic Collect on many devices, the forms can be included directly into the Medic Collect build. By doing so the forms are automatically loaded onto the phone when the app is first started, or whenever the forms are removed.

To add forms to a build, add them to the `assets/forms` folder, then rebuild Medic Collect to obtain a new .apk file. Media files, such as `form_logo.png`, can also be added. These would be added to the corresponding media folder. For example, if you want to have a custom image shown in a form `stock.xml` you would have `assets/forms/stock-media/form_logo.png`. All other media files needed for a form can be added in the `-media` folder.

### Settings
To deploy the application with settings preconfigured you will need to first install Medic Collect. Once it is installed, configure the Admin and General Settings you wish to have as default settings. You can even set the Admin Password so that the Admin Settings is blocked for users. 

Once the settings are configured as you wish them to be for your deployment, go to `Admin Settings`, and in the context menu select `Save Settings to Disk`. This will save your current settings to your device, and inform you of the location: `{{/sd_card_path}}/medicmobile/settings/collect.settings`. Copy the `collect.settings` file from your device to the `assets/` folder of your Medic Collect project, and rebuild the .apk.

The first time you install Medic Collect it will copy over the default settings from the `collect.settings` file. If you have changed settings on the phone and want to go back to these default setting, go to `Admin Settings`, and in the context menu select `Reload Default Settings`. This will reset the device to use the default settings that were included in the build (if any).
