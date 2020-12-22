# student20_pop
Proof-of-personhood, fall 2020

## overview
Back-end for the Proof-of-personhood application. To run the backend simply : `go run main.go <arguments>`. The possible 
arguments are :
* `-m` : the mode we want the server to run : either "o" for organizer or "w" for witness. Default to "o".
* `-a` : the IP address we want the server to run on. Default is empty.
* `-p` : the port we want the server to run on. Default is 8080.
* `-k` : the actor's public key. Default is "oui".
* `-f` : the file we want the backend to store its database on. Default is "org.db"

## Packages

### actors
This package implements the actors defined by the PoP project, and their reactions to received messages.

### db 
This package's purpose is to handle interaction with databases. The databases structure is described in the section "Back-end" components.

### network 
This package is to handle interactions with the websocket. It has a hub, which handles the publish-subscribe paradigm, and
the hub has an actor (either Organizer or Witness), that implements the PoP concept.

### event
This package provides structure for "real world events" and corresponding jsonRPC messages,  methods to convert byte received arrays into theses structures
and function to check validity of the messages and signatures we received.

### jsonRPC
This package is not used at all, but is defines the protocol we use to format the messages we send and receive.

### lib
This package defines gloabal useful functions, like find an element in an array, etc.

### message
This package implements the messages that can be sent or received as defined in the jsonRPC package.

### parser
This package implements all functions to parse received messages into structs, and compose messages from structs.

### security
This package implements all the checks we do on messages, from timestamp age to validity of hashes and signatures.

### test
This package is, as its name says it, for the tests.

## Back-end components

### Dependencies
Currently, the project works with the following libraries:
* [gorilla/websocket](https://github.com/gorilla/websocket): a websocket package for golang. We chose to use this 
package because it offers a good API, and it is more complete than the websocket package offered by the standard library.
* [boltdb](https://github.com/boltdb/bolt): a low-level key-value DB management system. It works by creating Buckets, 
that we can populate with key-value pairs.

### network testing 
If you want to test the websocket protocol the server running on a different machine than the client, you have to set the
server's IP address in `index.html`  at line 36 (instead of `localhost`). You'll have to put your computer's IP address
before the ":8080" of `main.go` at line 29.

### Database Structure
Currently, there are 2 databases created/opened when we run the backend: orgDatabase.db/witDatabase.db and sub.db.

#### Sub.db
This database exists only to handle publisher and subscribers of channels. Currently, we only manage subscribers. There is a bucket called
"sub", in which we store the (key, value) pairs as following : each key is the ID of one channel, and the value is an array of connections
that subscribed to this channel. When someone subscribes or unsubscribes, we simply edit this array.

#### witDatabase.db / OrgDatabase.db
This file has the same structure, whether it is witness or organizer. It has 2 types of bucket :
* The first one is called `channels` and for each channel it contains a (key, value) pair, where the key is the channel's ID
and the value is a string containing the "current state" of the channel. There is no history of messages stored here, only the channel's name, organizer, list of witnesses, etc...
* The second type of bucket is the bucket containing the history of the channel. There is one such bucket per channel, and 
it has the following properties :
    1. The bucket name is the ID of the channel
    2. The (key, values) pairs are (ID, content) of messages sent on the channel.

Once a message is stored, it shall not be updated, except for messages requesting witnesses signature, in which case we 
append the received signatures at the end of the orginial message's list of signatures.

   
### Json specifications

We use the protocol as defined in the branch `proto-specs`, which is in the `json-RPC` folder of the project.

### Useful links :
* https://stackoverflow.com/questions/20101954/json-unmarshal-nested-object-into-string-or-byte