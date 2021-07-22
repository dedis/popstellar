### PoP Go Backend

This repository contains the server side implementation of the PoP project.

#### Getting Started

We assume that you're familiar with the PoP project. A few resources to get an
idea about the overall design of the project are:

* [Architecture Specifications](https://docs.google.com/document/d/19r3rP6o8TO-xeZBM0GQzkHYQFSJtWy7UhjLhzzZVry4)
* [Protocol Specification Documentation](https://docs.google.com/document/d/1fyNWSPzLhM6W9V0VTFf2waMLiJGcscy7wa4bQlLkySM)

##### Requirements

Please ensure you have Go >= 1.16 installed on your machine. Instructions for
doing so are available [here](https://golang.org/doc/install).

Linux/OSX users also need to install GNU Make. OSX users may install it
using homebrew. Linux users may do so using their package manager:

```bash
brew install make # OSX
sudo apt-get install build-essential # Ubuntu/Debian
```

##### Resources

If this is your first time working with Go, please follow the following tutorials:

* [Getting Started](https://golang.org/doc/tutorial/getting-started)
* [Creating Modules](https://golang.org/doc/tutorial/create-module)
* [A Tour of Go](https://tour.golang.org/welcome/1)

 
##### IDE/Editors

Go is supported well across multiple text editors and IDE. The team at DEDIS
has members using [GoLand (IntelliJ)](https://www.jetbrains.com/go/), [VSCode](https://code.visualstudio.com/)
and neovim/vim.

VSCode/Neovim/vim require some custom configuration for adding Go support. We'd
suggest using GoLand if you do not have a strict preference/experience with the
other text editors since it works out of the box and EPFL/ETHZ students may avail
a [free education license](https://www.jetbrains.com/community/education/#students)
for their use.

#### Project Structure

The project is organized into different modules as follows

```
.
├── cli
│   ├── organizer       # cli for the organizer
│   └── witness         # cli for the witness
├── db                  # persistance module
├── docs
├── hub                 # logic for organizer/witness
├── message             # message types and marshaling/unmarshaling logic
├── network             # module to set up Websocket connections
├── test                # sample test data
└── validation          # module to validate incoming/outgoing messages
```

Depending on which component you're working on, the entry point would
either be cli/organizer or cli/witness, with bulk of the implementation
logic in the hub module.

#### Architecture

The PoP Go backend expects actors to establish long lived websocket connections
with it and send messages back and forth over websockets.

<div align="center">
  <img alt="Communication Stack" src="images/comm_stack.jpeg" width="600" />
</div>

The `Socket` interface (refer `hub/socket.go`) describes the methods used for
reading or sending data/error messages from/to the end user.

Depending on the type of end user, a `Socket` has three concrete implementations:

* `ClientSocket`: Used to denote a connection to a user participating in a PoP Party
* `WitnessSocket`: Used to represent a connection to a witness server.
* `OrganizerSocket`: Used to represent a connection to the organizer server.

The `ReadPump` and `WritePump` are low-level methods which allow
reading/writing data over the wire. Most users would instead use the `Send(msg []byte)`,
`SendError(id int, err error)` and `SendResult(id int, res message.Result)` APIs

Each incoming message read by `ReadPump` is passed to the `Hub interface`'s
(refer hub/hub.go) `Recv` method for processing

The `Hub` interface has two concrete implementations - one for the organizer
and another for the witness. Since both share common implementations they embed
the `baseHub` implementation with a few methods "overriden" where custom
implementation is needed.

The `baseHub` on receiving a message, processed it by invoking the
`handleIncomingMessage` method where its handled depending on which `Socket` the
message originates from.

The flowchart below describes the flow of data and how messages are processed

<div align="center">
  <img src="images/flowchart.png" alt="Flowchart"/>
</div>

<p align="center"><i>Credits to the be1-go Spring 2021 team for the flowchart</i></p>

The hubs themselves contain multiple `Channels` with the `Root` channel being
the default one, representing the LAO itself. Another example of a channel would
be one for an `Election`.

At the moment, the backend does *not* persist any data on disk and maintains
in-memory data structures for storing messages. This means all messages sent to
the server will be lost after the process exists. The `db` package does implement
a persistance layer which allows storing messages using SQLite but it remains
to be integrated.

##### Message definitions

All messages are defined in the `message` package along with the logic for
marshaling and unmarshaling them. Please note that the JSON-RPC definitions in
the root of the repository are to be considered a source of truth since the
validation library checks the messages against it.

Please refer to existing message types, `MarshalJSON` and `UnmarshalJSON` methods
to get an idea about how to implement a new type.

##### Validation

All the incoming messages are validated using the `validation` package. The
`make build` and `make test` commands automatically copy over the JSON-RPC specifications
and bundle it up during compilation.

#### Debugging Tips

* Be generous with the use of log statements while developing a new feature.
It's useful to get feedback about which steps executed and how far the message
reached in the processing pipeline rather than getting an opaque error.
* Ensure your error messages are descriptive.
* If you're stuck, using a debugger can be of great help. GoLand has good
support for it.

#### Deployments

Please reach out to the DEDIS Engineering team members to deploy a build to an
internet accessible host.

Alternatively, if you wish to test things, **strongly** consider using [ngrok](https://ngrok.com/)
to get an internet accessible URL which proxies requests to your local machine
directly. This is a great option when testing against mobile devices in a group
and if you need a quick turnaround time.

#### Coding Style

Go is opinionated about coding style and guidelines. As a rule of thumb, please
please run `make check` before submitting any Pull Request and ensure there are
no errors.

The CI also executes static analysis using SonarCloud which is good for giving
early feedback against common problems. Please ensure all the code smells and
warnings raised by SonarCloud are resolved before requesting reviews.
