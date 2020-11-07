# student20_pop
Proof-of-personhood, fall 2020

# overview
We defined the basic structure that mainly follow the publish subscribe model

# Folders

## Database 
The idea is to store a big string which contains all the information (basically in json format)
- Userdb 
   - store and retrieve user from database
   - parse user string(in class jsonHelper
- Channeldb:    
   - store and retrieve channel from database
   - parse chanel string(in class jsonHelper

## Src 
- Hub: The bandmaster of our backend
   - Serve() which basically will be the huge loop needed to run the server and everything
   - OpenConnection() 
   - ListenIncomingMsg()
   - Broadcast() 
   - SendMsg() 
- Subscriber:   
   - Publish data in database
   - Fetch from the server to get the updates of the channels
   - InterpretMessage which will decode string and  call the action method
   - ActAccordingly which will call the function needed from the user concerned 
- Publisher:    
   - Listen to the websocket 
   - Send message to hub

# Back-end components

## Dependencies
Currently, the project works with the following libraries:
* [gorilla/websocket](https://github.com/gorilla/websocket) : a websocket package for golang. We chose to use this 
package because it offers a good API, and it is more complete than the websocket package offered by the standard library.
* [boltdb](https://github.com/boltdb/bolt) : a low-level key-value DB management system. It works by creating Buckets, 
that we can populate with key-value pairs.

## WebSocket Protocol
The actual implementation (3 files) currently works as following :
* the `hub.go` maintains a list of opened connections and "routes" the messages. (Not very useful yet given there is only 1 connection)
* the `connection.go` acts like a server and serves on port 8080. When it receives a message from the client, it passes 
it to the hub for broadcast and stores it in the DataBase.
* the `index.html` acts like the client that prints messages received from the server and that can send messages to the server.
### tests 
If you want to test the websocket protocol the server running on a different machine than the client, you have to set the
server's IP address in `index.html`  at line 36 (instead of `localhost`). You'll have to put your computer's IP address
before the ":8080" of `main.go` at line 29.

## Database Structure
As we are using a very basic DBMS enabling only key-values storage, we decided to have the following structure for each "entity",
as listed in the [protocol messaging definition](https://docs.google.com/document/d/1jDyNAEEkkIkg4y2kxxELNGUi58CPThNbFrsvYEZiNXk/edit#) and
implemented in the `dataDefinition.go` file : 

up to 3 layers of nested buckets with, for the example above the LAO (key => value) pairs:

1. Layer (for each LAO):
    - count => the number of registered LAOs
    - LAO's ID => new Bucket containing all LAO's info
2. Layer (for each LAO's attribute):
    - OrganizerPKey => ...
    - Bucket containing a list of witnesses
    - new Bucket containing a list of members
    - new Bucket containing a list of events
    - Signature => ...
    - IP => ...
    - (token)?
3. Layer (for each LAO's witness/member/event)
    - 1 count => the count of registered witness/member/event
    - 2 => witness/member/event 's public key
    
## Json specifications

* Jsons keys must only be lowercase
* Every Json message must at least contain a field `"type"` (Lao, Event, Vote, ...) and a field `"action"` (get, set, ...)

### Json messages declaration
The following messages can be sent and require in addition the following fields :

**LAO:**
* Create : ```"name" , "organizerpkey", "ip" ```
* Get : ```"id"```
