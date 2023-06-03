# Karate Tests

This folder implements tests for the back-end and front-end.

## First Setup

### All
Make sure that you have [maven](https://maven.apache.org/download.cgi) installed
### Backend

To test a backend, you don't need more setup that what is needed to build that backend. Use the resources provided by
those projects.

### Android Front-end

To test the Android Frontend, you need to have an Android emulator installed. The easiest way to achieve it is to
install it through [Android Studio](https://developer.android.com/studio) :
Go to `Tools -> AVD Manager` and create an emulator.

Then you need to install Appium. You can install either the command line app with `npm install -g appium`, or
the [Desktop App](https://github.com/appium/appium-desktop/releases/).

Finally, you need to set the environment variable `ANDROID_HOME` (The previous name was`ANDROID_SDK_ROOT`
and it still works)
to your Android SDK installation. Find it by
opening Android Studio and going to `Tools -> SDK Manager`. It stands next to `Android SDK Location`.

If your Computer runs on Windows : we strongly advise that you do not use a VM or WSL. You will encounter problems you
would not have otherwise, some of which might even be technically impossible to solve.

### Web Front-end

Make sure you have [Google Chrome](https://www.google.com/intl/en/chrome/) and [npm](https://nodejs.org/en/download/)
installed.

## Running the Tests

### Backend

Build the backend you want to test. Follow the steps described in the corresponding subproject.

Simply run the tests for server-to-client communication with:

```
mvn test -DargLine="-Dkarate.env=go_client"
mvn test -DargLine="-Dkarate.env=scala_client"
```

Run the tests for server-to-server communication with:

```
mvn test -DargLine="-Dkarate.env=go_server"
mvn test -DargLine="-Dkarate.env=scala_server"
```

### Android Front-end

Build the application by running `./gradlew assembleDebug` in the corresponding directory.

Start the Android Emulator. Start Appium : if you use the GUI, delete the text in Host and Port and click on the start
server button. If you use the terminal, run `appium`.

With Android Bumblebee the emulator can either run in a tool window or a standalone window. (To have it in a standalone
window, go to `File -> Settings -> Tools -> Emulator` and unselect `Launch in a tool window`).
- Standalone window : \
The emulator window name should match : `Android Emulator - avd:id` \
Ex: `Android Emulator - Pixel_4_API_30:5554`
- Tool window : open the `Extended Controls` (the 3 points above the emulator)
  - The Extended Controls window name is the `avd` but with ' ' instead of '_' \
    Ex: `Pixel 4 API 30` for an `avd` of `Pixel_4_API_30`
  - Go to `Help`, under `Emulator ADB serial number` it should match `emulator-id`\
    Ex: `emulator-5554`



Make sure the [karate-config](src/test/java/karate-config.js) is correct. More precisely :

- `deviceName` is set to `emulator-<id>`, here it would be `emulator-5554`.
- `avd` is set to the avd name indicated on the emulator window, here it would be `Pixel_4_API_30`.

Run the test with :
`mvn test -DargLine="-Dkarate.env=android"`

### Web Front-end

Build the app with `npm run build-web` in the corresponding directory.

If your Chrome installation is not one of these :

- mac: `/Applications/Google Chrome.app/Contents/MacOS/Google Chrome`
- win: `C:/Program Files (x86)/Google/Chrome/Application/chrome.exe` \
  (You should check, it is also common for Chrome to installed in `/Programm Files/` rather than `/Program Files (x86)/`)

You need to set the executable manually in the driver definition in
the [Web page object](src/test/java/fe/utils/web.feature) (line 6).

Change the line from `* configure driver = { type: 'chrome' }`to `* configure driver = { type: 'chrome', executable: 'PATH' }`\
where PATH is the path to your Chrome installation.\

Run the test with :
`mvn test -DargLine="-Dkarate.env=web"`
