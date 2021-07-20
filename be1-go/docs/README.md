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

