# student21_pop: fe2-android
Proof-of-personhood, Fall 2021: Android native front-end

## Table of contents
* [Overview](#overview)
* [Documentation](#documentation)

## Overview

#### Setup

Import the project to [Android Studio](https://developer.android.com/studio) or [IntelliJ](https://www.jetbrains.com/idea/) with the Android Studio plugin.

In the AVD Manager you can create an emulator to run the app. For the M1 Apple Silicon Mac you need to go to *Other Images* when selecting a system image and choose an arm64 ABI image. Another option is to download [this](https://github.com/google/android-emulator-m1-preview) Android emulator.

#### Execution

There are two build variants on this project, *Prod* and *Mock*. To run the application choose the *prodDebug* variant. The CI uses the *mockDebug* variant to build and run the tests.

To run the application from the IDE you need to select the configuration *app* and the device. To run on an Android device connect it to the computer and run the following commands. Keep in mind that the minimum API required is 26.

```
./gradlew build
./gradlew installProdDebug
```

#### Tests

It is possible to select and run any android and unit tests from the IDE. Using the option *Run with Coverage* only informs the coverage of the unit tests. 

It is also possible to run the tests from the command line using the following command:

```
./gradlew connectedCheck
```

## Documentation

Detailed information about the implementation is available in the [docs](docs/README.md) directory.