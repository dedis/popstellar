# student20_pop: fe2-android branch
Proof-of-personhood, fall 2020: Android native front-end

## Table of contents
* [Technicalities](#technicalities)
* [Setup](#setup)

## Technicalities
* The [R class in Android](https://stackoverflow.com/questions/4953077/what-is-the-class-r-in-android) is an auto-generated class containing the resource IDs all the resources of res/directory.

## Setup
#### Usage
Import the project to Android Studio or IntelliJ and run the app.

To run on an Android device connect it to the computer and run the following commands.
```
gradle build
```
```
gradle installDebug
```
Find the app installed on the device and open it.

#### Test
```
gradle build
```
```
gradle check
```