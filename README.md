# student20_pop
Proof-of-personhood, fall 2020

Note that the main work in this repository is to be done
in the context of the following front-end and back-end branches:

* [fe1-web](https://github.com/dedis/student20_pop/tree/fe1-web): Web-based front-end implementation in ReactJS
* [fe2-android](https://github.com/dedis/student20_pop/tree/fe2-android): Android native front-end implementation in Java or Scala (TBD)
* [fe3-ios](https://github.com/dedis/student20_pop/tree/fe3-ios): Optional iOS native front-end implementation in Swift
* [be1-go](https://github.com/dedis/student20_pop/tree/be1-go): Back-end implementation in Go
* [be2-scala](https://github.com/dedis/student20_pop/tree/be2-scala): Back-end implementation in Scala

These branches were intentionally initialized as fresh Git repositories
with unrelated histories,
so that Git will complain about attempts to cross-merge them.
Please do not attempt merges from one branch to another (for now),
without careful consideration and discussion!
Treat them as if they were entirely separate and unrelated repositories for now.
We may develop a process for eventually merging them
into one ultimate, stable "master" repository
combining all the front-end and back-end builds, but that's for later if at all.

Everyone working on the project,
please create your own private "working" branches as needed
by forking the appropriate `fe*` or `be*` branch(es) you’re working on,
using the naming convention
`work-(fe*|be*)-<yourname>[-optional-variant]`.
For example,
a branch I create to contribute to the `fe1-web` project
might be called `work-fe1-bford` by default,
or `work-fe1-bford-random-experiment` if I need an additional temporary branch
for a random experiment for example.

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

## Database Structure
As we are using a very basic DBMS enabling only key-values storage, we decided to have the following structure for each "entity",
as listed in the [protocol messaging definition](https://docs.google.com/document/d/1jDyNAEEkkIkg4y2kxxELNGUi58CPThNbFrsvYEZiNXk/edit#) and
implemented in the `dataDefinition.go` file : 

up to 3 layers of nested buckets with, for the example above the LAO (key => value) pairs:

1. Layer (for each LAO):
    - count => the number of registered LAOs
    - LAO's ID => new Bucket containing all LAO's info
2. Layer (for each LAO's attribute):
<<<<<<< HEAD
    - ID => ...
=======
>>>>>>> work-be1-rpugin
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
    
