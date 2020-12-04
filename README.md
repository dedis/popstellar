# student20_pop
Proof-of-personhood, fall 2020

# overview
Back-end for the Proof-of-personhood application. To run the backend simply : `go run main.go <mode> <address>` where
`<mode>` can be either 'o' or 'w' (for organizer and witness respectively) and address is the adress and port on which 
the server should run (e.g. localhost:8080)

# Packages

## Database 
The idea is to store a big string which contains all the information (in json format)
- Channeldb:    
   - store and retrieve channel from database
   - parse chanel string(in class jsonHelper
   - keep record of subscribers of channels

## Websocket 
- Hub.go: The bandmaster of our backend
   - OpenConnection() and CloseConnection(), self explainatory, for websocket connections
   
- Connection.go: the websocket connection
    - reader(): read messages from websocket
    - writer(): writes messages to websocket
    - serveHTTP() : serves the http connexion for the websocket
    
- homePage.go
    - serveHTTP(): serves the index.html file
    
## Channel
- channel.go : the channel managment at the WS layer
    - subscribe() : subsribes a given WS to a given channel
    - unsubscribe() : unsubsribes a given WS from a given channel
    - getSubscribers(): returns the subscribers of a given channel
    - find(): a helper function to find items in slices
    
- dataDefinitions.go : defines structures for the used data types
    - LAO
    - Person
    - Event
    - Election
    - Vote
    
-jsonHelper.go : some helper functions to work with JSON



# Back-end components

## Dependencies
Currently, the project works with the following libraries:
* [gorilla/websocket](https://github.com/gorilla/websocket): a websocket package for golang. We chose to use this 
package because it offers a good API, and it is more complete than the websocket package offered by the standard library.
* [boltdb](https://github.com/boltdb/bolt): a low-level key-value DB management system. It works by creating Buckets, 
that we can populate with key-value pairs.

## WebSocket Protocol
The actual implementation (4 files) currently works as following :
* the `hub.go` maintains a list of opened connections and "routes" the messages. (Not very useful yet given there is only 1 connection)
* the `connection.go` acts like a server and serves on port 8080. When it receives a message from the client, it passes 
it to the hub for broadcast and stores it in the DataBase.
* the `index.html` acts like the client that prints messages received from the server and that can send messages to the server.
* the `homePage.go` just serves the `index.html` file on http requests
### tests 
If you want to test the websocket protocol the server running on a different machine than the client, you have to set the
server's IP address in `index.html`  at line 36 (instead of `localhost`). You'll have to put your computer's IP address
before the ":8080" of `main.go` at line 29.

## Database Structure
As we are using a very basic DBMS enabling only key-values storage. Currently everything is stored in one Database
(and file) called channel.db, and separated in 2 buckets.

There is one bucket ???@Raoul that stores key values pairswith channel's ID as keys and 
channel's JSON string representation as value.

There is also a channel "sub" that is only used by the publish subscribe protocol. Basically it contains channel IDs as
keys, just like above, but this time the value is the list of the subscribers to this particular channel.
    
## Json specifications

We use the protocol as defined in [Protocol - Detailed specifications](https://docs.google.com/document/d/1fyNWSPzLhM6W9V0VTFf2waMLiJGcscy7wa4bQlLkySM/edit).

## Useful links :
* https://stackoverflow.com/questions/20101954/json-unmarshal-nested-object-into-string-or-byte