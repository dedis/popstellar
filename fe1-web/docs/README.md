# PoP Web Frontend

This repository contains the client side implementation of the PoP project.

## Getting Started

We assume that you're familiar with the PoP project. Please read the
[Architecture Specifications](https://docs.google.com/document/d/19r3rP6o8TO-xeZBM0GQzkHYQFSJtWy7UhjLhzzZVry4)
to get an idea about all the actors and components in the system.

### Resources

If this is your first time working with TypeScript, React and/or Redux,
please follow the following tutorials:

* [TypeScript Handbook](https://www.typescriptlang.org/docs/handbook/intro.html)
* [Introduction to React](https://reactjs.org/tutorial/tutorial.html)
* [Redux Essentials](https://redux.js.org/tutorials/essentials/part-1-overview-concepts)

### IDE/Editors

TypeScript is supported well across multiple text editors and IDE.
The team at DEDIS has members using
[WebStorm (IntelliJ)](https://www.jetbrains.com/webstorm/),
[VSCode](https://code.visualstudio.com/)
and neovim/vim.

VSCode/Neovim/vim require some custom configuration for adding TypeScript and React support.
We'd suggest using WebStorm if you do not have a strict preference/experience
with the other text editors since it works out of the box and EPFL/ETHZ students may avail a
[free education license](https://www.jetbrains.com/community/education/#students)
for their use.

## Project Structure

The project is organized into different modules as follows

```
.
├── App.tsx                    # the entry point
│
├── __mocks__                  # test mocks for external libraries
├── __tests__                  # tests
│
├── components                 # library of reusable UI component (React-based)
│
├── ingestion                  # module to process incoming messages
│
├── model                      # module containing the data model & objects
│    ├── network               # ... of the objects sent over the network
│    └── objects               # ... of the application's entities
│
├── navigation                 # module dealing with the React navigation
│
├── network                    # module to network with the backend
│
├── parts                      # module containing the application's unique screens
│
├── res                        # resources (assets, images, etc.)
│
├── store                      # module dealing with application state (Redux-based)
│    ├── reducers              # module containing the reducers (Redux)
│    └── stores                # module to access states outside the React environment
│
├── styles                     # stylesheets
└── utils                      # interfaces to system libraries
     └── __mocks__             # mocks of those libraries, to be used in tests
```

## Architecture

The PoP TypeScript frontend provides an interface to allow the users to
interact as part of a Local Autonomous Organization (LAO),
within which they will acquire Proof-of-Personhood tokens to identify themselves.

Under the hood, the frontend establishes one or more long-lived websocket
connections with the servers (organizers, witnesses), through which messages
are sent back and forth using a publish/subscribe pattern.

On a higher level of abstraction, the frontend may publish messages or subscribe
for messages on a *channel*. You may think of a channel as a topic which
stores events that occur over time. For instance, every LAO is denoted by a unique
channel (also called the LAO channel) and contains messages about all events
that occur within it, for example, the creation of an election, a roll call.
A channel may have sub channels associated with it. For instance, a
LAO may have a sub-channel for the elections where all messages associated
with that election are published. Please refer to
[Data pipeline architecture](https://docs.google.com/document/d/19r3rP6o8TO-xeZBM0GQzkHYQFSJtWy7UhjLhzzZVry4/edit#heading=h.1h71fzpdznrh)
for more information.

All the messages are encoded using JSON and are validated using
[JSON Schemas](https://json-schema.org) as defined in the
[protocol folder](https://github.com/dedis/student_21_pop/tree/master/protocol).

[Protocol Specifications](https://docs.google.com/document/d/1fyNWSPzLhM6W9V0VTFf2waMLiJGcscy7wa4bQlLkySM)
also gives an introduction to the different message formats. However, note that
the [schemas](https://github.com/dedis/student_21_pop/tree/master/protocol) in this
repository are **always** the source of truth and are more up to date than the Google Doc.

### Managing the application state

In order for the application to perform as a user would expect, it needs to manage
its own internal state robustly. Its internal state is made up of local data,
such as a user's cryptographic material and preferences, but also of the local
representation of the application's view of the whole PoP system and its state.

The latter needs to be eventually consistent, i.e., sooner or later all frontend
users obtain the same "view" of the system. This translates to a basic expectation
that, one would expect all devices connected to a LAO to "see" the same thing.
Drawing a parallel, one would expect all social media users to be able to access
the same posts, see (roughly) the same number of associated likes, etc.

In order to achieve this, and as a general rule, the UI displays information
from the `store` module (the application state container),
but it **doesn't** modify the LAO-wide information contained within it.
The view of the LAO, contained in the application state,
only gets updated in response to messages from the backends.

As such, let's take the example of a user who wants to publish or modify LAO-wide information.
In our example, the user wants to cast a vote in an election and does the necessary UI operations.
In turn, the application will send a message to the backend (which the backend should acknowledge),
which will then validate it and propagate it in the system.
Eventually, the vote is sent back to the application (through the publish/subscribe channel),
and upon receiving it the application would update its state.
By doing so, the store would contain new information that would automatically be reflected in the UI.

<div align="center">
  <img alt="Architecture" src="images/architecture-data-flow.png" width="600" />
</div>

Occasionally, the user interface could directly modify the application state,
but this would only be valid for local operations affecting local data
(e.g., changing a local setting, clearing the data stored in the browser, etc.).

For more information on managing the application state, please refer to the
[store module](https://github.com/dedis/student_21_pop/tree/master/fe1-web/store)
and make sure you have a solid understanding of Redux and its concepts.

#### Managing & storing secrets

A particular sub-problem of storing the application data is the management of secret data.
While any secret could be encrypted, this just shifts the problem to securely storing
cryptographic material and making sure that it is not leaked by the device running the application.

As the Web front-end is built on React and designed to also support a React Native deployment on
iOS and Android mobile phones, secret management must be conceived in a multi-platform way,
taking into account and abstracting the differences between these platforms.
Both mobile devices and the browsers offer APIs to manage cryptographic material securely,
but their support is limited to specific cryptographic primitives,
which unfortunately do not include those used in the PoP protocol specification.
Notably, most APIs do not support the Ed25519 keys.

As such, the general approach is to use platform-specific secure key management solutions,
such as the [Android KeyStore](https://developer.android.com/training/articles/keystore),
to safely store keys that will then be used to encrypt and decrypt the application secrets,
including the Ed25519 cryptographic material.
This ensures that all application secrets are encrypted-at-rest,
that the encryption and decryption keys are stored securely,
and that the application is free to use any convenient cryptographic primitive.

On the browser, which offers an intrinsically challenging security environment,
this translates to the use of the
[Web Crypto API](https://developer.mozilla.org/en-US/docs/Web/API/Web_Crypto_API)
to generate non-extractable key material, and the
[IndexedDB](https://developer.mozilla.org/en-US/docs/Web/API/IndexedDB_API)
to store it reasonably securely.
This is implemented in [WalletCryptographyHandler](TBD).

Support for mobile devices is planned and the architecture allows it,
but secret management is not yet implemented.

### Sending messages over the wire

The communication stack within the PoP project is made of
[multiple layers](https://docs.google.com/document/d/1AeV7JX_SJ30mu9PIwmz24UkIi3jCo6NSYA0sPxdbscU)
and you need to be familiar with them to understand how communication happens.

The [network module](https://github.com/dedis/student_21_pop/tree/master/fe1-web/network)
contains most of the logic to encapsulate application-level messages and pass them down the stack,
and it is organized as follows, going from the lowest abstraction to the highest:
* `NetworkManager` and `NetworkConnection` classes abstract away the intricacies of
  dealing with WebSocket-based connections and errors that may appear at that level of abstraction.
* `JsonRpcAPI` exposes the [JSON-RPC](https://www.jsonrpc.org/specification) -based API
  that provides the publish/subscribe communication abstraction. In it, you will find
  functions to publish a message, subscribe and unsubscribe to a channel, and so on.
  **Importantly**, the publishing of messages also takes care of the `Message` layer,
  effectively encapsulating the application-level message as needed.
* `MessageApi` exposes the application-level message generation logic,
  in order to simplify the logic in the application code and reduce repetition.
* `CommunicationApi` contains utility functions to deal with common operations,
  such as subscribing to a channel and retrieving past messages.

For more information on sending messages on the network, please refer to the
[network module](https://github.com/dedis/student_21_pop/tree/master/fe1-web/network)
and make sure you have a solid understanding
of [JSON-RPC](https://www.jsonrpc.org/specification),
the [Protocol Specifications](https://docs.google.com/document/d/1fyNWSPzLhM6W9V0VTFf2waMLiJGcscy7wa4bQlLkySM)
and their actual implementation in the [protocol schemas](https://github.com/dedis/student_21_pop/tree/master/protocol)

### Getting messages over the wire

Once it is clear how to send messages over the wire,
it is important to turn one's attention to receiving them.

Because receiving messages over the network and processing them to update the application state
are very different steps involving completely unrelated logic,
the two operations are split in separate and independent modules.

On the networking side, the `NetworkManager` and `NetworkConnection` classes
have facilities to define and attach a `JsonRpcHandler` callback (see `network/RpcHandler.ts`)
to a websocket connection.
This callback is called whenever the backend sends a JSON-RPC request or notification.

On the "message processing" side,
the [ingestion module](https://github.com/dedis/student_21_pop/tree/master/fe1-web/ingestion)
is the module responsible for receiving the messages and processing them,
effectively "ingesting and digesting" them (hence the name).
It is solely responsible for forwarding incoming messages in the `store`,
processing them to update the application state as needed,
and finally marking them as processed within the `store` itself.

Within the `ingestion` module, the `handlers` submodule is responsible for
processing different kind of messages, based on their types.

For more information on processing messages received from the network, please refer to the
the [ingestion module](https://github.com/dedis/student_21_pop/tree/master/fe1-web/ingestion)
and make sure you have a solid understanding of [Redux](https://redux.js.org/)
and the PoP communication protocol.

### Message definitions

All objects referred to in the protocol specification (and the logic for parsing them)
are defined in `model/network` package, closely mirroring the JSON-Schema folder structure.

Please note that the JSON-RPC definitions in the root of the repository are to be considered
a source of truth since the validation library checks the messages against it.

When you need to create a new object, please refer to existing message types and in particular
their `fromJSON` method to get an idea about how to implement a new type.

### User Interface

The development of an application providing a good user interface, enabling a good user experience,
requires the user interaction patterns to be predictable, coherent and homogenous across the app.
To achieve this, applications typically resort to the definition and implementation of a library of
reusable components - building bricks - that are reused throughout the application and assembled
to create the different "views" (or "screens") of the application.

As you design a given screen, you will break it down into sub-components, sub-sub-components, etc.
You can refer to the [React documentation](https://reactjs.org/docs/thinking-in-react.html#step-1-break-the-ui-into-a-component-hierarchy)
to have guidance on the thinking process going into this component break-down.

In general, the top-level components (the "views" or "screens") will be full of
application-specific logic, as they'll encompass the full behavior of that screen.
As you go down into sub- and sub-sub-components, you'll come across more generic logic,
which is only dealing with the UI itself.

As part of the PoP Web front-end, you'll find the low-level reusable UI components in the
`/components` package, while the main views and screens of the application are found in `/parts`.

In concrete terms, the `/components` package provides elements such as a
[Date Picker](https://github.com/dedis/student_21_pop/tree/master/fe1-web/components/DatePicker.tsx),
a [QR Code displayer](https://github.com/dedis/student_21_pop/tree/master/fe1-web/components/QRCode.tsx),
or a component managing
[a list of Text Inputs](https://github.com/dedis/student_21_pop/tree/master/fe1-web/components/TextInputList.tsx).

The `/parts` package, on the other hand, contains screens such as the
[Wallet setup](https://github.com/dedis/student_21_pop/tree/master/fe1-web/parts/wallet/WalletSetSeed.tsx)
and the
[Wallet home](https://github.com/dedis/student_21_pop/tree/master/fe1-web/parts/wallet/WalletHome.tsx).

This is the current organization, but as this project evolves you should feel free to reorganize the
code layout (in agreement with your project teammates and the TAs) in any way that is convenient.

For more information on developing the user interface, please make sure you have a solid
understanding of [React](https://reactjs.org/).

#### Linking User Interface and Application State

For more information on linking up together the user interface with the application state,
please make sure you have a solid understanding of [React Redux](https://react-redux.js.org/)
and look at existing components.

#### Navigation

The navigation is handled in the `navigation` package.

For more information on managing the navigation in the user interface,
please make sure you have a solid understanding of [React Navigation](https://reactnavigation.org/).

### Validation

All the incoming messages are validated using the `network/validation` package,
which ensures that all constraints defined within the JSON Schema are respected.

Furthermore, each object has specific constraints defined within its constructor.
For an example, see `model/network/method/message/data/rollCall/CreateRollCall.ts`.

## Testing

Software testing is a vast topic, requiring lots of application-specific adaptations.
We will not detail here aspects of software strategy, lest you would be reading a book
instead of a README, but you will find below some resources that you could find useful
in ensuring the code you write conforms to the specifications and your expectations:

- [Testing React applications with Jest](https://jestjs.io/docs/tutorial-react)
- [Understanding Mocking in Jest](https://medium.com/@rickhanlonii/understanding-jest-mocks-f0046c68e53c)
- [Testing application state management with Redux](https://redux.js.org/usage/writing-tests)

These resources mostly relate to unit and integration testing, but End-to-End (E2E) testing
has its own dedicated set of tools:

- [Selenium](https://www.selenium.dev/) is the most famous one, but it has a low-level API
- [NightWatch.js](https://nightwatchjs.org/) offers a wrapper around Selenium and alternatives,
making it straightforward to write E2E tests for an application.

If you write E2E tests, please be mindful of making them maintainable through the use of the
[Page Object Model](https://martinfowler.com/bliki/PageObject.html).

## Debugging Tips

* Make abundant use of the Chrome Development Tools, they offer full visibility in
  the network activity (HTTP requests, WebSocket messages, etc.), they enable you
  to put breakpoints anywhere in your code and allow you to inspect the browser storage.
* Take it a step further with the "React Developer Tools" and "Redux DevTools" plugins,
  which give you full visibility into the structure of your GUI, and the events
  that changed the application state, respectively.
* Be generous with the use of log statements while developing a new feature.
  It's useful to get feedback about which steps executed and how far the message
  reached in the processing pipeline rather than getting an opaque error.
* Ensure your error messages are descriptive.

## Deployments

Please reach out to the DEDIS Engineering team members to deploy a build to an
internet accessible host.

Further information will be made available at a later date. -- @pierluca, 04.09.2021

## Coding Style

TypeScript leaves a lot of freedom to the developer, so it takes extra effort to maintain
a consistent coding style. The codebase was developed based on the TypeScript variant of
the [AirBnB style guide](https://github.com/airbnb/javascript).
As a rule of thumb, please run `npm run eslint` before submitting any Pull Request
and ensure there are no errors. This will run ESLint, a JavaScript/TypeScript linter
that will check your code for common errors and adherence to the style guide.

The CI also executes static analysis using SonarCloud which is good for giving
early feedback against common problems. Please ensure all the bugs, code smells
and warnings raised by SonarCloud are resolved before requesting reviews.

### Common pitfalls

It can be very tempting to make ESLint errors "go away" by simply disabling a check
on a troublesome line of code. This is also generally the wrong solution.
In the majority of cases, ESLint is pointing to a genuine problem,
sometimes of a stylistic nature, sometimes running deeper and affecting software stability.

Please try to address the underlying issue before disabling an ESLint check,
and if you still feel that disabling a check is the right approach,
do discuss your choice with your teammates and/or the TAs before proceeding.
