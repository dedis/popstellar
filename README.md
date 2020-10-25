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



## Coding convention
Our styling convention regarding the components can be found [here](https://thoughtbot.com/blog/structure-for-styling-in-react-native)
Our coding convention follow the aibnb, that can be found [here](https://github.com/airbnb/javascript) and a quick install guide [here](https://medium.com/pvtl/linting-for-react-native-bdbb586ff694)
