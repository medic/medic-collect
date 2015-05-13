# medic-collect

##Overview

These instructions should help you get setup using Eclipse to develop on Medic Collect, building JavaRosa from source.
These notes were taken during an initial setup of Medic Collect using Eclipse, please update as needed.

##Instructions

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
