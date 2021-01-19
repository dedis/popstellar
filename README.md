# student20_pop
Proof-of-personhood, fall 2020. Go back-end version, by Romain Pugin, Raoul Gerber and Ouriel Sebbagh.

## Overview
Golang back-end for the Proof-of-personhood application. To run the backend simply : `go run main.go <arguments>`. The possible 
arguments are :
* `-m` the mode we want the server to run : either "o" for organizer or "w" for witness. Default to "o".
* `-a` the IP address we want the server to run on. Default is empty (localhost).
* `-p` the port we want the server to run on. Default is 8080.
* `-k` the actor's public key. Default is "oui".
* `-f` the file we want the backend to store its database on. Default is "org.db" for an organizer and "wit.db" for a witness

You can generate the code documentation using the godoc command: `godoc -http:=6060`.

## Packages
### actors
This package implements the actors defined by the PoP project (organizer and witness), and their reactions to received messages. 
The "actor" interface defines what functions are mandatory for an actor to implement.

### db 
This package's purpose is to handle interaction with the local database. The database's structure is described in the section [Back-end components](#Database-structure).

### network 
This package's purpose is to handle the webserver listening for websocket connections and to handle them.
It has a type `Hub`, that decides which message to send to whom according to the publish-subscribe paradigm.
The hub has an actor (either Organizer or Witness), that implements the PoP concept, and generate response messages 
depending on the received message.

### event
This package provides the structures for real world events. We use it when storing the different components internally.

### message
This package provides structures corresponding to the serialized messages that can be sent or received as defined in the jsonRPC package.

### jsonRPC
This package is not directly used by this backend version, but defines the messaging protocol used to on the websockets.

### lib
This package defines global useful functions, like finding an element in an array, etc.

### parser
This package implements all functions to parse received messages into structs, and compose messages from structs.

### security
This package implements all the checks we do on messages to ensure they are valid, from timestamp age to validity of hashes and signatures.
* Concerning the Hash when dealing with the Ids, the structure for concatenation is as follows:
`Hash(element1|...|elementK)` is Hash("["esc(element1)",...,"esc(elementK)"]") where esc is an escaped function defined in the protocol.
  The fields concatenated are all in their received representation, e.g. `message_id` will be the hash of the concatenation of the two field `data` and
                                                                        `signature` both encoded in base 64 because we receive them encoded.
* For the signature, we exclusively use the unencoded representation. 

## Back-end components
### Dependencies
Currently, the project works with the following libraries:
* [gorilla/websocket](https://github.com/gorilla/websocket) is a websocket package for golang. We chose to use this 
package because it offers a good API, and it is more complete than the websocket package offered by the standard library.
* [boltdb](https://github.com/boltdb/bolt) is a low-level key-value DB management system. It works by creating Buckets, 
that we can populate with key-value pairs.

### Database structure
When running a back-end server, the program will check if the database specified as argument already exists. If it does 
it will open it, and will create it otherwise. The database will have the exact same structure whether you run the server 
as an organizer or as a witness. The contents may differ, but the structure stays the same.

The database is going to store 2 types of bucket:
* The first type is called `channels`, and it is a unique bucket. For each channel, it contains a (key, value) pair where the key is the channel's ID
and the value is a string containing the current state of the channel. There is no history of messages stored there, 
only the channel's name, organizer, list of witnesses, etc...
* The second type of bucket are buckets containing the history of one channel. There is one such bucket per channel, and 
it has the following properties :
    1. The bucket name is the ID of the channel
    2. The (key, values) pairs are (ID, content) of messages sent on the channel. The content is the field `message` that
  is contained in the field `params` of the received json message.

Once a message is stored, it shall not be updated, except for messages requesting witness signing. In that case, we 
append the received witness signatures at the end of the original message's list of signatures.

## Unit-tests
Some unit-tests exist directly in their corresponding packages, as per Go specification. They can be run with `go test -v ./...` (recursive from the top-level folder) or simply `go test` for the current package.

## Basic testing
We provide a very basic front-end in the file `index.html`. It does nothing but opens a websocket connection to the back-end server, and lets
you send and receive messages.

If you want to test the application with the `index.html` front-end on another machine as the back-end server, you will have to change the
back-end server's IP address (and port) in `index.html` at line 36 (instead of `localhost`). You'll also have to run the back-end
server with this IP address (and port) as the `-a` (and `-p`) flags. However, as this works with IPv4, make sure both machines
are in the same LAN.

### Json specifications
The protocol we use is defined in the branch `proto-specs`, and is a git submodule incorporated in the `json-RPC` folder.

## Potential improvements
### Database
The library we currently use is not maintained anymore, this is why we should consider upgrading to https://github.com/etcd-io/bbolt
It would also be great to switch to something more complete than a key-value storage, like for instance switching to a 
simple relational DB, like e.g. SQLite. This would provide features that we currently do not support like indexing on 
different values than ID, sort query results by fields, etc.

### Project Structure
Currently, the project biggest, and "all-containing" entity is the Hub (package network). Then the hub has an actor 
(either organizer or witness). It would make more sense to turn things around, in order to make the Actor the
principal entity, and to let the actor have a hub.

### WebSockets
Maybe we should consider using websocket secure (websocket over TLS) instead of "standards" websocket (over TCP). It 
would prevent from MitM attacks, as we currently don't offer data privacy.