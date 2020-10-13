# student20_pop: fe1-web branch
Proof-of-personhood, fall 2020: Web-based front-end

## To run the project
Open the terminal in the folder `PopApp` and enter the following commands

```bash
npm install
npm start
```

> In case of an Exception *Tried to register two views with the same name NRCSafeAreaProvider*, [follow these steps](https://github.com/th3rdwave/react-native-safe-area-context/issues/110#issuecomment-660407790) :
>
> - remove `node_modules` folder
> - run `npm install`
> - remove `node_modules/expo/node_modules/react-native-safe-area-context` folder
> - run `expo r -c`
>
> The following times, using `expo start` is enough



Then it will open a web page and you will need to scan the QR code with your phone with the Expo app.
