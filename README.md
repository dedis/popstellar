# student20_pop
Proof-of-personhood, fall 2020

# Packages

## db 
This package contains helper functions to interact with databases. A big code refactor will be pushed soon and this package will absorb the current channel package.

## Websocket 
- Hub.go: The bandmaster of our backend
- Connection.go: the websocket connection
- homePage.go: serves the index.html file
    
## Define    
- dataDefinitions.go : defines structures for the used data types
- errorHelpers.go : defines the error used inside the backend
- jsonHelpers.go : structures of messages received through the websocket and functions to extract data from jsons
- helper.go : general useful functions that are not in the go std library (find elem in slice for now)
- securityHelpers.go : functions to check hashes and signatures


# Back-end components

## Dependencies
Currently, the project works with the following libraries:
* [gorilla/websocket](https://github.com/gorilla/websocket): a websocket package for golang. We chose to use this 
package because it offers a good API, and it is more complete than the websocket package offered by the standard library.
* [boltdb](https://github.com/boltdb/bolt): a low-level key-value DB management system. It works by creating Buckets, 
that we can populate with key-value pairs.

## WebSocket Protocol
The actual implementation (4 files) currently works as following :
* the `hub.go` maintains a list of opened connections and "routes" the messages. We will change this so there is no more routing to do in the next update.
* the `connection.go` is the file that properly handles the websocket connection. When it receives a message from the client, it passes 
it to the hub.
* the `index.html` acts like the client that prints messages received from the server and that can send messages to the server.
* the `homePage.go` just serves the `index.html` file on http requests
### tweaking the server's properties 
If you want to test the websocket protocol the server running on a different machine than the client, you have to set the
server's IP address in `index.html`  at line 36 (instead of `localhost`). You'll have to put your computer's IP address
before the ":8080" of `main.go` at line 29.

## Database Structure
As we are using a very basic DBMS enabling only key-values storage. There are currently 3 different databases :
1. for the hub, containing a list of channels and their subscribers
2. one for the witness (described below)
3. one for the organizer (same structure as witness's one)

### Organizer/Witness Database
Thoses databases contains 2 types of bucket :
* The first type is a unique bucket named containing, for each channel, a key-value pair with the channel's ID as key and all the infos about that channel as value. No history is stored in this bucket.
* The 2nd type of bucket is a bucket whose name is the id of a channel, and that contains a key-value pair for each message sent on this channel. The keys are the message's ID and the values the data contained in this message.

## Json specifications

We use the protocol as defined in the branch proto-specs on github.