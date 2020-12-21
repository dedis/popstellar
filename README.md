# student20_pop: fe1-web branch
Proof-of-personhood, fall 2020: Web-based front-end

## Running the project
Open the terminal in the folder `PopApp` and enter the following commands

```bash
npm install
npm start
```

> If the Exception *Tried to register two views with the same name NRCSafeAreaProvider*, [follow these steps](https://github.com/th3rdwave/react-native-safe-area-context/issues/110#issuecomment-660407790) :
>
> - delete  `node_modules` folder
> - run `npm install`
> - delete  `node_modules/expo/node_modules/react-native-safe-area-context` folder
> - run `expo r -c`
>
> The following times, running the command  `expo start` is enough



`expo start` automatically starts **Metro packager**. 

- Opening the project on a phone requires to scan the QR code on the bottom left corner of the page and having [Expo Client app](https://expo.io/tools) installed to display the project. Without the app, a phone may be simulated using the emulator/simulator buttons on the packager
- Opening the project on a browser requires to click on the *run in web browser* button on the packager

---



## Standalone application mode

The app can run normally without any associated backend by running the `LocalMockServer.js` and connecting the websocket link to the mock server instead of a real backend.

The mock server can be run from the `PopApp` folder using:

- `npm run startServer` or
- `npm run startMonitorServer` if [nodemon](https://www.npmjs.com/package/nodemon) is installed globally on the host

`startMonitorServer` will restart the mock server automatically every time the source code changes

---



## Unit tests

Unit tests can be run from the `PopApp` folder using:

- `npm test` or
- `./node_modules/mocha/bin/mocha --recursive`

---



## Modules npm choices

- **app dependencies**:
  - we use a combination of [redux](https://redux.js.org/) and [redux-persist](https://github.com/rt2zz/redux-persist) to store our persistent data in the browser _localStorage_. This method will not work for native users (Android, iOS, ...). A solution could be to use the [_AsynchronousStorage_](https://github.com/react-native-async-storage/async-storage) and rethink part of the implementation (see this [pull request](https://github.com/dedis/student20_pop/pull/121) for more information);
  - we use a combination of [tweetnacl](https://www.npmjs.com/package/tweetnacl) and [tweetnacl-util](https://github.com/dchest/tweetnacl-util-js) to handle everything related to cryptography (key generation, signatures, ...). This method will not work for native users (Android, iOS, ...). A solution could be to use [tweet-nacl-react-native-expo](https://www.npmjs.com/package/tweet-nacl-react-native-expo) (both libraires in one) which works on a native device but not on a browser. This would imply using a conditional import depending on the device type which we didn't manage to implement without various runtime errors.
- **dev dependencies**:
  - we use [ESlint](https://eslint.org/) to check that our coding convention is respected.

---



## Coding convention

Our styling convention regarding the components can be found [here](https://thoughtbot.com/blog/structure-for-styling-in-react-native)
Our coding convention follow the aibnb, that can be found [here](https://github.com/airbnb/javascript) and a quick install guide [here](https://medium.com/pvtl/linting-for-react-native-bdbb586ff694)