# student21_pop: fe2-android
Proof-of-personhood, spring 2021: Android native front-end

## Table of contents
* [Technicalities](#technicalities)
* [Setup](#setup)
* [Github Actions](#github-actions)
* [Coding Standards](#coding-standards)

### Note

This repository is being refactored and some classes and interfaces marked with `@Deprecated` annotation are due for removal.

## Technicalities
* The target API is 29 and the minimum required is 26
* The [R class in Android](https://stackoverflow.com/questions/4953077/what-is-the-class-r-in-android) is an auto-generated class containing the resource IDs all the resources of res/directory
* The application follows the MVVM pattern, and uses this [guide to app architecture](https://developer.android.com/jetpack/guide)
* The [ViewModel](https://developer.android.com/topic/libraries/architecture/viewmodel) classes implement the UI logic, retrieve and manage the data that is needed
* The WebSockets are implemented using the [Scarlet](https://github.com/Tinder/Scarlet) library, one of its usage on this application is in the [Injection](https://github.com/dedis/student_21_pop/blob/master/fe2-android/app/src/prod/java/com/github/dedis/student20_pop/Injection.java) class
* The application uses [RxJava](https://github.com/ReactiveX/RxJava) for asynchronous and event based programs
* The [Room](https://developer.android.com/reference/androidx/room/package-summary) library is used to define the application's database, entities and Data Access Objects

#### Usage
Import the project to Android Studio or IntelliJ, choose an emulator in the AVD Manager and run the app. The emulator used for running and testing was a Pixel 2 API 29.
If working on a M1 Apple Silicon Mac, [this](https://github.com/google/android-emulator-m1-preview) Android emulator can be downloaded and used.

To run on an Android device connect it to the computer and run the following commands. Keep in mind that the minimum API required is 26.
```
gradle build
```
```
gradle installDebug
```
Find the app installed on the device and open it.

#### Test

*Note:* The tests are temporarily broken as a result of refactoring.

Open the virtual device and run the following command:
```
./gradlew connectedCheck
```
It's also possible to run the tests using the option "Run with Coverage" from Android Studio.


## Github Actions

*Note:* CI has been disabled during refactoring.

This project uses Github Actions as a CI, for more information go to the [workflows](https://github.com/dedis/student_21_pop/blob/master/.github/workflows/ci.yaml) of this project.

This CI builds and runs the Unit Tests. For the Android Tests, the [reactivecircus](https://github.com/ReactiveCircus/android-emulator-runner) Android Emulator is used, which is limited. There are issues finding the resource values and checking [Toast messages](https://developer.android.com/reference/android/widget/Toast) appearance.

The Jacoco plugin can be set in the future for code coverage, this [guide](https://www.raywenderlich.com/10562143-continuous-integration-for-android#toc-anchor-013) covers how to set it.

## Coding Standards

This project follows the [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html). The [google-java-format](https://github.com/google/google-java-format) plugin allows very easy formatting.

Please ensure that you configure Android Studio to use `google-java-format`. As of now, v1.9 of the plugin requires JDK11 which is not shipped with Android Studio 4.0. Please install
the `Choose Runtime` plugin from the Marketplace and install a JDK11 runtime by double pressing shift and searching for `Choose Runtime` in the popup.

`google-java-format` does not handle import orders unfortunately. Please import the [google-style scheme](https://raw.githubusercontent.com/google/styleguide/gh-pages/intellij-java-google-style.xml)
in Android Studio by going to `Preferences` -> `Editor` -> `Code Style` -> `Import Scheme...` on the Gear Icon for Scheme.

Finally, you may want to install `Save Actions` plugin and configure it to `Optimise Imports` and `Reformat File` on save.

### Resource Values
The values used for the UI are stored in the corresponding xml
files (colors, dimens, strings or styles) in the res/values folder.

They can then be accessed using ```R.id``` or ```getResources()```.

The strings and dimensions are divided by usage, for example all strings or dimensions used for tabs are grouped together.


