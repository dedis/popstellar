# student_21_pop
Proof-of-personhood, Spring 2021. Go back-end version.

## Overview

You may build the `pop` CLI to interact with the server by executing `go build -o pop ./cli/`.

```bash
./pop organizer -h
NAME:
   pop organizer - manage the organizer

USAGE:
   pop organizer command [command options] [arguments...]

COMMANDS:
   serve    start the organizer server
   help, h  Shows a list of commands or help for one command

OPTIONS:
   --public-key value, --pk value  base64 encoded organizer's public key
   --help, -h                      show help (default: false)

```

You may start the server at port `9000` by executing `./pop organizer --pk "<base64 encoded pk>" serve`.
Please use the `-p` flag to specifiy an alternative port.

## Packages

- `cli`: Entrypoint for starting the PoP server
- `message`: Contains structs that are used to marshal/unmarshal websocket payload
- `hub`: Defined an interface for the actions supported by the server. Refer to the interfaces in `hub/hub.go` for more information.


## Dependencies
Currently, the project works with the following libraries:
* [dedis/kyber](https://github.com/dedis/kyber) which adds support for cryptographic operations like signature verification.
* [gorilla/websocket](https://github.com/gorilla/websocket) is a websocket package for golang. We chose to use this 
package because it offers a good API, and it is more complete than the websocket package offered by the standard library.


### Sample Data

Sample test data for different websocket payloads is available at `test/json_test_strings.txt`.

## Unit-tests
Some unit-tests exist directly in their corresponding packages, as per Go specification. They can be run with `go test -v ./...` (recursive from the top-level folder) or simply `go test` for the current package.


## Potential improvements

### WebSockets
Maybe we should consider using websocket secure (websocket over TLS) instead of "standards" websocket (over TCP). It 
would prevent from MitM attacks, as we currently don't offer data privacy.
