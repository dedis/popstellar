# Karate Tests

This folder implements tests for the back-end and front-end. The front-end tests are out of date and currently not expected to pass.

## Architecture
### General
Karate test cases are called Scenarios. They are structured with the following keywords:

- Given: Prepare the JSON payload to be sent to the component being tested. 
- When: Defines the action that is to be performed with the payload. For instance, publish to publish a message containing some high-level message data.
- And: Connector that can be used after any of the other keywords.
- Then: Asserts that the action taken in the 'When' step has the expected outcome.

Scenarios are grouped in different feature files that each tests a different message type (i.e. electionOpen, createRollCall etc.). Code defined in the background section of a feature file will be run before each scenario. This can be used to set up previous steps of a test, for example opening an election before testing a close election message.

In the background section, `read(classpath: "path/to/feature")`to make the current feature share the same scope as the called feature. This means they share definitions (def variables) and configurations. JavaScript functions defined in the called feature are available for use in the current feature's context.


### Java code 
We can instanciate Java Classes inside features. For instance, the mockClient feature provides functions to create mock front-ends or back-ends that are instanciations of the Java class MockClient. This class represents a WebSocket client that also has the ability to create valid message data, such as for example using the function createValidLao, createValidRollCall etc.

### Data model
To generate valid message data for JSON payloads, a simplified model of the system (Lao, RollCall, Elections etc.) is implemented in Java code. Instances of MockCLients use this model to create message data that is valid for the 

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
mvn test -DargLine="-Dkarate.env=go"
mvn test -DargLine="-Dkarate.env=scala"
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
