# Karate Tests

This folder implements tests for the back-end and front-end.

## First Setup

### Backend

To test a backend, you don't need more setup that what is needed to build that backend. Use the resources provided by
those projects.

### Android Front-end

To test the Android Frontend, you need to have an Android emulator installed. The easiest way to achieve it is to
install it through [Android Studio](https://developer.android.com/studio) :
Go to `Tools -> AVD Manager` and create an emulator.

Then you need to install Appium. You can install either the command line app with `npm install -g appium`, or
the [Desktop App](https://github.com/appium/appium-desktop/releases/).

Finally, you need to set the environment variable `ANDROID_SDK_ROOT` to your Android SDK installation. Find it by
opening Android Studio and going to `Tools -> SDK Manager`. It stands next to `Android SDK Location`.

### Web Front-end

Make sure you have [Google Chrome](https://www.google.com/intl/en/chrome/) installed.

## Running the Tests

### Backend

Build the backend you want to test. Follow the steps described in the corresponding subproject.

Simply run the tests with:

```
mvn test -DargLine="-Dkarate.env=go"
mvn test -DargLine="-Dkarate.env=scala"
```

### Android Front-end

Build the application by running `./gradlew assembleDebug` in the corresponding directory.

Start the Android Emulator and Appium.

The emulator window name should match : `Android Emulator - avd:id` \
Ex: `Android Emulator - Pixel_2_API_30:5554`

Make sure the karate-config is correct, more precisely :

- `deviceName` is set to `emulator-<id>` where id is the one found on the emulator window, here it would be `emulator-5554`.
- `avd` is set to the avd name indicated on the emulator window, here it would be `Pixel_2_API_30`.

Run the test with :
`mvn test -DargLine="-Dkarate.env=android"`

### Web Front-end

Build the app with `npm run build-web` in the corresponding directory.

If your Chrome installation is not one of these :

- mac: `/Applications/Google Chrome.app/Contents/MacOS/Google Chrome`
- win: `C:/Program Files (x86)/Google/Chrome/Application/chrome.exe`

You need to set the executable manually in the driver definition in
the [Web page object](src/test/java/fe/utils/web.feature) (line 6).

Change the line from `* configure driver = { type: 'chrome' }`to `* configure driver = { type: 'chrome', executable: 'PATH' }`\
where PATH is the path to your Chrome installation.

Run the test with :
`mvn test -DargLine="-Dkarate.env=web"`
