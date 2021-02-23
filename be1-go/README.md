# student_21_pop
Proof-of-personhood, Spring 2021. Go back-end version.

## Overview

You may build the `pop` CLI to interact with the server by executing `go build -o pop cli`.

## Packages

- `cli`: Entrypoint for starting the PoP server
- `message`: Contains structs that are used to marshal/unmarshal websocket payload
- `hub`: Defined an interface for the actions supported by the server. Refer to the interfaces in `hub/hub.go` for more information.


### Dependencies
Currently, the project works with the following libraries:
* [gorilla/websocket](https://github.com/gorilla/websocket) is a websocket package for golang. We chose to use this 
package because it offers a good API, and it is more complete than the websocket package offered by the standard library.


## Unit-tests
Some unit-tests exist directly in their corresponding packages, as per Go specification. They can be run with `go test -v ./...` (recursive from the top-level folder) or simply `go test` for the current package.


## Potential improvements

### WebSockets
Maybe we should consider using websocket secure (websocket over TLS) instead of "standards" websocket (over TCP). It 
would prevent from MitM attacks, as we currently don't offer data privacy.
