# popstellar: fe1-web
Proof-of-personhood, web-based front-end

## Running the project
Open the terminal in the `fe1-web` folder and enter the following commands

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

> If 'npm install' fails because of sharp (verify by running 'npm install -g sharp-cli').
> [This post](https://stackoverflow.com/questions/68710308/sharp-error-package-openexr-was-not-found-in-the-pkg-config-search-path) may be helpful



`expo start` automatically starts **Metro packager**.

- Opening the project on a phone requires to scan the QR code on the bottom left corner of the page and having [Expo Client app](https://expo.io/tools) installed to display the project. Without the app, a phone may be simulated using the emulator/simulator buttons on the packager
- Opening the project on a browser requires to click on the *run in web browser* button on the packager

## Retrieving the public key
In order to retrieve the public key that is used by the frontend, open the javascript console of the web browser of your choice. [This stackoverflow answer](https://webmasters.stackexchange.com/a/77337) gives an overview of how to do so in different browsers if you have never done this. Then look for the log entry `Using the public key: XXX`.

---



## Standalone application mode

The app can run normally without any associated backend by running the `LocalMockServer.js` and connecting the websocket link to the mock server instead of a real backend.

The mock server can be run from the `fe1-web` folder using:

- `npm run startServer` or
- `npm run startMonitorServer` if [nodemon](https://www.npmjs.com/package/nodemon) is installed globally on the host (`npm install -g nodemon` will install the package **g[lobally]**)

`startMonitorServer` will restart the mock server automatically every time the source code changes

---



## Unit tests & Linting

Unit tests can be run from the `fe1-web` folder using:

- `npm [run] test` or
- `npm run test -- --silent` to disable console logs

ESlint feedback can be seen directly from your IDE or using the command `npm run eslint`

---



## Modules npm choices

- **app dependencies**:
  - we use a combination of [redux](https://redux.js.org/) and [redux-persist](https://github.com/rt2zz/redux-persist) to store our persistent data *asynchronously* in the browser _localStorage_ (or a localStorage polyfill for mobiles, old browsers, and tests);
  - we use a combination of [tweetnacl](https://www.npmjs.com/package/tweetnacl) and [tweetnacl-util](https://github.com/dchest/tweetnacl-util-js) to handle everything related to cryptography (key generation, signatures, ...). This method will not work for native users (Android, iOS, ...). A solution could be to use [tweet-nacl-react-native-expo](https://www.npmjs.com/package/tweet-nacl-react-native-expo) (both libraires in one) which works on a native device but not on a browser. This would imply using a conditional import depending on the device type which we didn't manage to implement without various runtime errors.
  - we use [react-navigation](https://reactnavigation.org/) to handle all the navigations between the different views. The package works great with the native platforms, but as some design issue in the web.
- **dev dependencies**:
  - we use [ESlint](https://eslint.org/) to check that our coding convention is respected.

---



## Coding convention

Our coding convention is based on airbnb, which can be found [here](https://github.com/airbnb/javascript) and a quick install guide [here](https://medium.com/pvtl/linting-for-react-native-bdbb586ff694)

Our convention for styling React elements can be found [here](https://thoughtbot.com/blog/structure-for-styling-in-react-native)
