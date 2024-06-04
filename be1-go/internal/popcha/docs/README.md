# PoPCHA Go Backend

The `popcha` package contains the Go implementation of the PoPCHA Authorization server

## Overview

This documentation doesn't describe the high-level architecture or logic of PoPCHA, but rather its specific
implementation in Go, and its integration in the PoP System.

The PoPCHA backend consists of several interconnected components:
* an HTTP server
* Websocket connections on the `/response` endpoint of the HTTP server
* JavaScript WebSocket clients per page served by the HTTP server
* a Channel, used to receive and handle authentication messages from the PoP App

The code specific to PoPCHA can be found in the following directories:

```
.
├── channel             # contains the abstract definition of a channel
│   ├── authentication  # channel implementation for an authentication channel
│   └── [...]           # other channels
├── [...]
├── popcha              # HTTP server and back-end logic for PoPCHA
│   ├── docs            # PoPCHA-specific documentation
│   ├── qrcode          # html code for the QRcode-serving webpage
│   ├── server.go         # implementation of the PoPCHA HTTP server
│   └── server_test.go           # test suit for the PoPCHA server    
```

## Using PoPCHA

The instructions for launching the PoPCHA server can be found in the `README.md` in the `be1-go` root directory. 
A command line interface allows you to configure the host address and port of the server.

## Multi-server communication

When launching servers in parallel, please make sure to configure distinct ports for the PoPCHA servers, as the default
port is `9100`, and should be unique for the given host address.

