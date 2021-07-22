# student_21_pop
Proof-of-personhood, Spring 2021. Go back-end version.

## Overview

You may build the `pop` CLI to interact with the server by executing
`make build` on Linux/OSX or `make.bat build` in Windows.

```bash
./pop organizer -h
NAME:
   pop organizer - manage the organizer
   pop witness - manage the witness

USAGE:
   pop organizer command [command options] [arguments...]
   pop witness command [command options] [arguments...]

COMMANDS:
   serve    start the organizer or witness server
   help, h  Shows a list of commands or help for one command

OPTIONS:
   --public-key value, --pk value          base64url encoded organizer's public key
   --organizer-address value, --org value  organizer's address and port for witness to connect to organizer (default value "localhost:9002")
   --client-port value, --cp value         port on which to open websocket for clients (default value 9000 for organizer, 9002 for witness)
   --witness-port value, --wp value        port on which to open websocket for witnesses (default value 9002)
   --other-witness value, --ow value       address and port on which to connect to another witness, can be used as many times as necessary
   --help, -h                              show help (default: false)

```

You may start the organizer server at ports `9000` for clients and `9001` for witnesses by executing `./pop organizer --pk "<base64url encoded pk>" serve`.
Please use the `-cp` and `-wp` flags to specify an alternative port.
The full path to connect to the organizer as a client is `ws://host:clientport/organizer/client/` and as a witness `ws://host:witnessport/organizer/witness/`.

You may start the witness server at ports `9000` for clients and `9002` for witness, connected to the organizer at `localhost:9000` by executing `./pop witness --pk "<base64 encoded pk>" serve`.
Please use the `-cp` and `-wp` flags to specify an alternative port.
Use the -org flag to specify an alternative address and port for the organizer.
Using the `-ow` flag as many times as necessary, you can specify you can specify the `address:port` of each of the other witnesses.
The full path to connect to the organizer as a client is `ws://host:clientport/organizer/client/` and as a witness `ws://host:witnessport/organizer/witness/`.

## Unit-tests
Some unit-tests exist directly in their corresponding packages,
as per Go specification. They can be run with `make check`
(recursive from the top-level folder) or simply `go test` for
the current package (except the `validation`).

## Documentation

Detailed information about the architecture and dataflow is available in
the [docs](docs/README.md) directory.

You may also make use of package level documentation by using `godoc`

```bash
$ godoc -http=:6060
```

The above command would make this module's documentation available at
[http://localhost:6060/pkg/student20_pop](http://localhost:6060/pkg/student20_pop).

## Potential improvements

### WebSockets
Maybe we should consider using websocket secure (websocket over TLS) instead of "standards" websocket (over TCP). It
would prevent from MitM attacks, as we currently don't offer data privacy.
