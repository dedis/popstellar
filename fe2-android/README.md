# popstellar: fe2-android
Proof-of-personhood, Fall 2021: Android native front-end

## Running the project

### Setup

Import the project from the folder `fe2-android` to [Android Studio](https://developer.android.com/studio) or [IntelliJ](https://www.jetbrains.com/idea/) with the Android Studio plugin.

In `Tools` -> `AVD Manager` you can create an emulator to run the app. For the M1 Apple Silicon Mac you need to go to *Other Images* when selecting a system image and choose an arm64 ABI image. Another option is to download [this](https://github.com/google/android-emulator-m1-preview) Android emulator.

### Execution

There are two build variants on this project, *Prod* and *Mock*. In the menu, go to `Build` -> `Select Build Variant...` to open the `Build  Variants` view, then choose *prodDebug* as the `Active Build Variant` to run the application. The CI uses the *mockDebug* variant to build and run the tests.

To run the application from the IDE you need to select the configuration *app* and the device on the toolbar. To run on an Android device connect it to the computer and run the following commands from the `fe2-android` folder. Keep in mind that the minimum API required is 26.

```
gradle build
gradle installProdDebug
```

It is also possible to build a debug APK that can be installed in any Android device with API greater than 26.
```
gradle assembleDebug
```

## Android and Unit Tests

It is possible to select and run any android and unit tests from the IDE. Using the option *Run with Coverage* only informs the coverage of the unit tests. 

It is also possible to run the tests from the command line by opening the terminal in the `fe2-android`folder and using:

```
gradle connectedCheck
```

## Documentation

Detailed information about the implementation is available in the [docs](docs/README.md) directory.