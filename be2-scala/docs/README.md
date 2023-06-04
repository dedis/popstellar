# PoP Scala backend

This repository contains the **scala server side implementation** of the PoP project.


## 1.	Getting Started

We assume that you're familiar with the PoP project. Please read the [Architecture Specifications](https://docs.google.com/document/d/19r3rP6o8TO-xeZBM0GQzkHYQFSJtWy7UhjLhzzZVry4) to get an idea about all the actors and components in the system.


### Resources

The code heavily relies on the principles of **actors** and **streams**. The first one is implemented using standard akka actors, whereas the latter uses akka DSL flow graphs. Finally, we use **spray-json** in order to properly encode/decode JSON messages.

- [Scala documentation](https://www.scala-lang.org/files/archive/api/2.13.1/)
- [Akka actors](https://doc.akka.io/docs/akka/current/typed/index.html)
- [Akka streams (DSL graphs)](https://doc.akka.io/docs/akka/current/stream/stream-graphs.html)
- [Spray-json](https://github.com/spray/spray-json): simple parsing library

Keep in mind that the [Pre-semester work project](https://docs.google.com/document/d/1gGRNVSKO4NGe2zzvVs28qJzkoMgIScq1iAQQct980pg) is a good tutorial to get familiar with both [WebSocket](https://en.wikipedia.org/wiki/WebSocket) and [Akka HTTP](https://doc.akka.io/docs/akka-http/current/index.html)


### IDE/Editors

The two most common Scala IDEs are VSCode and IntelliJ. Both are viable since we are using Scala version *2.13.x* (< 3.x).


## 2.	Project Structure

```
.
├── Server.scala 						# the entry point
│
├── json 								# module describing object & protocol json encoding/decoding
│
├── model 								# description of server's objects
│	├── network 						# JSON-rpc communication protocol model
│	└── objects 						# low-level server objects
│
└── pubsub 								# module handling the publish/subscribe model
	│
	├── ClientActor.scala 				# client model as an akka actor
	├── PubSubMediator.scala 			# handles pubsub channel operations
	├── PublishSubscribe.scala 			# DSL graph implementation
	│
	└── graph							# DSL graph description
		├── handlers 					# graph messages handlers
		└── validators 					# graph messages validators
```


## 3.	Architecture

The PoP Scala backend is used by LAO organizers and witnesses in order to store and validate LAO information/participation. Note that the actual implementation allows server to server and client to multi server communication; meaning that a LAO can be ran on multiple servers concurrently. A simplified version of the project is as follows: clients (either organizers, witnesses, or attendees) may connect to the server using WebSockets in order to "read" or "write" information on a database depending on their role in the LAO.

<div align="center">
  <img alt="Simplified be2 project architecture" src="images/be2-simplified.png" width="600" />
</div>


In more details, the whole backend is a giant [DSL graph](https://doc.akka.io/docs/akka/current/stream/stream-graphs.html) (see image below). Whenever a new request is received (blue "Source" circle), a new `ClientActor` is automatically created for each new connected client or server. This actor *represents the fundamental link between a particular client and the server*; any message sent to the actor will arrive directly in the client's mailbox.

The `ClientActor` then transmits data destined for the server directly to a partitioner for further examination. This partitioner will decide which path a particular message will follow (e.g. a JSON-rpc query will not be treated the same way as a JSON-rpc response).

Once the message has been processed (e.g. LAO created if the message is valid or reject the request if it is not valid or does not follow our custom JSON-rpc protocol), the assigned handler (green, orange or yellow boxes) asks the `AnswerGenerator` to inform the client whether the operation was successful or not.

<div align="center">
  <img alt="Simplified be2 project architecture" src="images/be2-s.png" />
</div>


If we look even closer, here is how the real be2 DSL graph is designed.

Between the `ClientActor` and the partitioner sits a module which goal is to validate conformity with our custom JSON-rcp protocol, decode the JSON payload, and then finally validate its fields.

The partitioner decides which path a message is supposed to take depending on its content; more precisely, depending on if it is a request, if it contains a message field, it will be forwarded to the paramsWithMessage handler, otherwise it will be filtered depending on the method carried by the json. Otherwise, if it is a response, it will be forwarded to the ResponseHandler. Further down the line, a handler is used for each type of message (e.g. the LAO handler is able to understand and process LAO messages such as `CreateLao`, `StateLao`, and `UpdateLao`)

The results are then collected by the main merger (blue "merger" circle) and sent to the `AnswerGenerator`. An answer (`JsonRpcResponse`) is created and sent back to the `ClientActor` (and thus the real client through WebSocket) by the `Answerer` module.

<div align="center">
  <img alt="Simplified be2 project architecture" src="images/be2-graph.png" />
</div>


### Sending and Receiving Messages

`ClientActor` is complex yet wonderful piece of code that resembles black magic at first sight. It *is* conceptually both the (websocket) link between the server & a particular client or server, as well as the actual internal representation of the client or server. Each different client or server is represented by a unique `ClientActor`. It serves as both the entry point (receiving a `JsonRpcRequest`) and exit point (sending a `JsonRpcResponse` to a client or broadcasting a `Broadcast` to multiple clients) of the graph.

#### `ClientActor` as entry point

Using a mix of akka http (handling low-level websocket stuff) and akka stream, we collect input client websocket messages as a string directly in the graph

```scala
// the flow of messages (which are websocket messages != our Message case class) is generated by akka http's handleWebSocketMessages function (see Server.scala)
val input = builder.add(
  Flow[Message].collect { case TextMessage.Strict(s) => println(s">>> Incoming message : $s"); s }
)
```

#### `ClientActor` as exit point

By sending a `ClientActor.ClientAnswer` to `ClientActor`, the latter will automatically send the JSON payload over websocket back to its corresponding client. This forward link is setup at graph creation in the `Answerer`. In short, any graph message that exits the graph (line 4) is mapped into a `ClientAnswer` and sent to the `ClientActor` (line 8)

```scala
// Integration point between Akka Streams and the above actor
val sink: Sink[GraphMessage, NotUsed] = Flow[GraphMessage]
  // Create a ClientAnswer from the input graph message
  .collect { case graphMessage: GraphMessage => ClientAnswer(graphMessage) }
  // Send the ClientAnswer to clientActorRef. Whenever the stream between the client
  // actor and the actual client (front-end) is broken, the message DisconnectWsHandle
  // is sent to clientActorRef
  .to(Sink.actorRef(clientActorRef, DisconnectWsHandle, { t: Throwable => println(t); DisconnectWsHandle }))
```


### Message Definition

All objects referred to in the protocol specification (and the logic for parsing them)
are defined in `model/network` package, closely mirroring the JSON-Schema folder structure.

Please note that the JSON-RPC definitions in the root of the repository are to be considered
a source of truth since the validation library checks the messages against it.

:information_source: When you need to create a new data case class (e.g. `CreateLao.scala`), please refer to existing message case classes. In order to tell the encoder/decoder how to encode/decode the new message (e.g. with `buildFromJson`), its "recipe" must be added directly in the `json/MessageDataProtocol.scala` file. Finally, an entry should be added to the `MessageRegistry` for your new data (object, action) pair containing the location of a schema verifier, builder, validator, and handler. Here's an example of such entry:

```scala
register.add(
  (ObjectType.LAO, ActionType.CREATE),
  SchemaValidator.createSchemaValidator("dataCreateLao.json"),
  CreateLao.buildFromJson,
  LaoValidator.validateCreateLao,
  LaoHandler.handleCreateLao
)
```

:information_source: You might also need to define a new `ObjectType` or `ActionType`  depending on your needs


### Spray-json Conversion

Whenever a new JSON-rpc message is added in the protocol, its encoding/decoding recipe should be added in this package. If the new message is a case class, spray-json is able to handle message conversion using `jsonFormatX`, where `X` corresponds to the number of parameters the case class has. For example,

```scala
case class A(i: Int, s: String)

implicit val format: RootJsonFormat[A] = jsonFormat2(A)
```

or

```scala
case class B(price: Double)
case class A(name: String, b: B)

implicit val formatB: RootJsonFormat[B] = jsonFormat1(B) // (1)
implicit val formatA: RootJsonFormat[A] = jsonFormat2(A) // (2)
```

:warning: Note that order matters! In order to convert `A`, (2) gets called. It first handles the `name` parameter (`String`) and then converts `b` (`B`). Thus the conversion for `B` (here (1)) has to be defined *above*.

:warning: Spray-json is *not* able to automatically convert "complex" types such as `Option[T]`, `Either[A, B]`, ... Examples of such cases are provided in the codebase.



On the other hand, if the new message is anything but a case class, you will need to specify the entire conversion by hand with the help of two methods: `read` (from JSON to internal representation) and `write` (from internal representation to JSON). Note that, once again, order matters. For example,

```scala
class Point(var x: Int, var y: Int) {}

implicit object Format extends RootJsonFormat[Point] {
	override def read(json: JsValue): Point = {
		json.asJsObject.getFields("x", "y") match {
			case Seq(JsNumber(x), JsNumber(y)) => Point(x.toInt, y.toInt)
			case _ => throw ...
		}
	}

	override def write(obj: Point): JsValue = JsObject(
		"x" -> obj.x,
		"y" -> obj.y
	)
}

```

Once again, a lot of examples of such cases are present within the codebase

:information_source: *tips*: always try to prioritize *case classes* whenever possible :)


### Validation

All the incoming messages are validated in a two-steps process using the `pubsub/graph/validators` package:
1. Check conformity with the custom JSON-rpc protocol using `[...]validators/Validator.scala:schemaValidator`;
2. Check information validity (e.g. correct signature) using the rest of the `[...]validators` package.

Each additional message constraint (e.g. in this particular case, "start_time" should always equal "end_time") is checked in the corresponding validator (e.g. `LaoValidator`) before the case class instance representing the message is created.

### Storage

We are using [leveldb](https://github.com/codeborui/leveldb-scala) in order to store messages "inside" channels. A channel is represented with a "path name" style prefix of structure `root/lao_id/...`.

Multiple messages may then be stored "inside" each channel (all within one single database file). Since leveldb is a key-value database, we are using `channel#message_id` as key and the corresponding `message` (in its json representation) as value.

The database is split between data stored as Key -> List[Message ids] and the data itself Key -> Message. Hence we are using two prefixes to the keys used to query the database :
- CHANNEL_DATA_KEY = "ChannelData:" for data stored as Key -> List[Message ids]
- DATA_KEY = "Data:" for the data itself.

Summary of the keys used to retrieve data:
- for a message: `Data:channel#message_id`
- for ChannelData: `ChannelData:channel`
- for LaoData: `Data:root/lao_id#laodata`
- for RollCallData: `Data:root/lao_id/rollcall`
- for ElectionData: `Data:/root/lao_id/private/election_id`
- for a SetupElection message: `Data:SetupElectionMessageId:channel`
- for a CreateLao message: `Data:CreateLaoId:channel`

We use `/` as a separator for parts of a channel and `#` as a separator for data objects when needed.

The database API is separated in two layers:
- leveldb API layer: implements the `Storage` trait (e.g. `DiskStorage` in the codebase) and communicates direcly with the underlying key-value database;
- proxy layer: `DbActor` is an akka actor that may be queried by most nodes in the graph. It uses the standard [akka ask pattern](https://doc.akka.io/docs/akka/current/typed/interaction-patterns.html#request-response) and it oblivious to the low-level database implementation the API layer provides (e.g. the fact that we are using leveldb)

```scala
def read(key: String): Option[String]
def write(keyValues: (String, String)*): Unit
def delete(key: String): Unit
def close(): Unit
```

As we can write multiple elements in the database in a single request, the `DiskStorage` uses a WriteBatch (also from [leveldb](https://github.com/codeborui/leveldb-scala)) to be able to store all that information atomically and prevent incoherences inside the database due to write errors.

```scala
val batch: WriteBatch = db.createWriteBatch()
    try {
      for (kv <- keyValues) {
        batch.put(kv._1.getBytes(StandardCharsets.UTF_8), kv._2.getBytes(StandardCharsets.UTF_8))
      }
      db.write(batch)
    } catch {
      case ex: Throwable => throw DbActorNAckException(
        ErrorCodes.SERVER_ERROR.id,
        s"could not write ${keyValues.size} elements to DiskStorage : ${ex.getMessage}"
      )
    } finally {
      batch.close()
    }
```
This example has been extracted from `pubsub/storage/DiskStorage.scala`.

`DbActor` is an akka actor and thus "understands" messages defined as case classes in `DbActor.scala:Event`. Examples of such messages include:

```scala
// DbActor Events correspond to messages the actor may receive
sealed trait Event

final case class Write(channel: Channel, message: Message) extends Event
final case class Read(channel: Channel, id: Hash) extends Event
```

`DbActor` will then answer using one of its predetermined answers defined within the same file:

```scala
// DbActor DbActorMessages correspond to messages the actor may emit
sealed trait DbActorMessage

final case class DbActorAck() extends DbActorMessage
final case class DbActorReadAck(message: Option[Message]) extends DbActorMessage
final case class DbActorCatchupAck(messages: List[Message]) extends DbActorMessage
```

When the `DbActor` fails to complete an operation – instead of sending a `DbActorMessage` –, the actor will send back to the origin a wrapped `DbActorNAckException` (containing both the error code and reason the exception occurred). The wrapper itself is a [`Status.Failure`](https://doc.akka.io/japi/akka/current/akka/actor/Status.Failure.html) often used by akka to notify a different actor that something went wrong.

:information_source: Note that the `Status.Failure` takes a `Throwable` at construction. You should **not** throw the exception inside the Failure unless you feel the urge to waste 3 hours debugging an incomprehensible actor state (definitely did not go through this :smile_cat:...)!

Let's take a look at the Write event from `DbActor`, where we use `this.synchronized` to avoid concurrency issues for sequential read/writes that should be seen as a single transaction. In this example, we are writing both the message on the channel (line 5) and the message_id in the ChannelData object (stored at 'channel', line 4).

```scala
this.synchronized {
  val channelData: ChannelData = readChannelData(channel)
  storage.write(
    (channel.toString, channelData.addMessage(message.message_id).toJsonString),
    (s"$channel${Channel.DATA_SEPARATOR}${message.message_id}", message.toJsonString)
  )
}
```

Here's an example (shamefully stolen from `MessageHandler.scala`) showing the power of `DbActor` coupled with Scala [Future](https://www.scala-lang.org/files/archive/api/2.13.1/scala/concurrent/Future.html)

```scala
val askWrite = dbActor ? DbActor.Write(rpcMessage.getParamsChannel, m)
askWrite.transformWith {
  case Success(_) => Future(Right(rpcMessage))
  case _ => Future(Left(PipelineError(ErrorCodes.SERVER_ERROR.id, s"dbAskWrite failed : could not write message $message", rpcMessage.id)))
}
```

There are also methods to read ChannelData for a given channel and LaoData for the Lao to which a channel belongs.
We also have the possibility to modify the LaoData (stored at `root/lao_id#laodata`) with a separate event called `WriteLaoData`.

```scala
final case class ReadLaoData(channel: Channel) extends Event
final case class ReadChannelData(channel: Channel) extends Event
final case class WriteLaoData(channel: Channel, message: Message) extends Event

final case class DbActorReadChannelDataAck(channelData: ChannelData) extends DbActorMessage
final case class DbActorReadLaoDataAck(laoData: LaoData) extends DbActorMessage
```

:warning: for now, the LaoData also stores the private key used by the Lao to sign broadcast messages, however, there is a security issue, as anyone able to use `ReadLaoData()` can therefore access the private key.

For the Social Media functionality, each user has their own channel with the identifier `root/lao_id/own_pop_token` and each broadcast containing the message_id of a post will be written to `root/lao_id/posts`.

For the Election functionality, we need to have a key pair stored safely somewhere so that we can encrypt/decrypt messages. That is why we use a `ElectionData` object to store the key pairs for the corresponding election.
The path root is `root/lao_id/private/election_id` as stated above.
The key pair can be stored and retrieved by the following functions.

```scala
final case class CreateElectionData(id: Hash, keyPair: KeyPair) extends Event
final case class ReadElectionData(electionId: Hash) extends Event

final case class DbActorReadElectionDataAck(electionData: ElectionData) extends DbActorMessage
```

The RollCallData is an object that stores the id and state of the latest rollcall action (`CREATE`, `OPEN`, `REOPEN`, or `CLOSE`). It ensures that we cannot open a closed rollcall or close a non-opened rollcall.
The stored parameters can be modified or retrieved by the following functions.

```scala
final case class ReadRollCallData(laoId: Hash) extends Event
final case class WriteRollCallData(laoId: Hash, message: Message) extends Event

final case class DbActorReadRollCallDataAck(rollcallData: RollCallData) extends DbActorMessage
```

:information_source: the database may easily be reset/purged by deleting the `database` folder entirely. You may add the `-Dclean` flag at compilation for automatic database purge


### Servers consistency

Making sure that servers that run a LAO concurrently share the same state is ensured by `Heartbeat` messages and `GetMessagesById` messages. These messages are proper to server to server communication and they basically follow the same path as the other messages in the DSL graph.
:warning: connection to other servers is internally represented by an instance of `ClientActor`. Since this can easily lead to errors, the way we distinguish a server to server connection and a client to server connection is by a `isServer` field in the `ClientActor` instance.
The way we broadcast heartbeats to connected servers is by mean of the `Monitor` Actor. The monitor actor sees every message the system receives. It schedules heartbeats either whenever it sees a message in the next heartbeatRate seconds, or periodically after a period of messageDelay seconds.
Heartbeats are sent by the mean of the `ConnectionMediator` actor that holds a set of connected server peers.




## 4.	GitHub introduction

### Import the project

`cd` where you want to have your project located and then clone the project using the following commands

```bash
git clone https://github.com/dedis/popstellar.git [folderProjectName]
```

We then want to navigate to the `be2-scala` subfolder

```bash
cd <folderProjectName>/be2-scala
```

You can then open the project using your favorite IDE (e.g. `idea .` for IntelliJ and `code .` for VSCode)

---

### Create a new branch

By default, the new branch you create will be a copy of `master`

```bash
git checkout -b <branchName>
```

If you rather want to make a copy of branch `x` (e.g. a branch that is currently being reviewed, i.e. not merged), use this instead

```bash
git checkout <x>
git checkout -b <branchName>
```

---

### Push changes on the remote server

When pushing changes, you first want to **commit** the changes you've applied using

```bash
git add <fileName>					# add file <fileName> to the commit
git commit -m"<message>"    # add a title to the commit (switch <message> for the actual message)

## Note: You may also add files separately using
git add <fileName>

## Note: You may also add all modified (!= new) files to the commit and set a title using
git commit -am"<message>"

## Note: You may also set a title and description for a specific commit using your favorite IDE (obviously doom emacs ^^). Then save and quit
git add <fileName>
git commit
```

<div align="center">
  <img alt="Example of git commit command" src="images/example-git-commit.png" width="700" />
</div>

:information_source: Do not create a commit with 2.000 lines of codes! Short commit with meaningful titles/description are way better and way easier to review

:information_source: There are faster ways to add multiple files in the commit such as `git commit <-u|.>`. You can check the corresponding man page for more information

You then finally want to pull/push the changes

```bash
git pull			# safety check to see if no one applies changes to your branch in your absence
git push			# push the commited changes
```

Note that if you just created the branch, you'll need to tell git once where to push the changes using the following command instead of `git push`

```bash
git push -u origin <branchName>
```

:warning: You cannot directly push changes on `master`! For that, you need to go through the process of creating a pull request (see below)

---

### Merge a branch into another

Navigate (using `git checkout <branchName>`) to the branch you want the merge **to be applied to** (i.e. the branch that we merge another into!). Then perform the following command to merge the content of branch `x` into the current checked out branch

```bash
git merge <x>
```

---

### Create a pull request

Creating a pull request (PR) can only be done with on the [GitHub website](https://github.com/dedis/popstellar/pulls) by navigating on "Pull requests". Click on the big green button titled "New pull request" and choose the receiving branch (generally `master`) as well as the branch you want to merge into the latter (e.g. `work-be2-scala-raulinn-test`). Click on "Create pull request".

You can then set a title and description to your PR as well as set a label (e.g. `be2-scala`), an assignee (the person responsible for the code), a project (if any) and ask for a specific reviewer using the right side bar. Click on "Create pull request"

## 5.	Debugging Tips

The best way to "intercept" a `GraphMessage` being processed in the graph is to launch the server in debug mode, and then sending an isolated message to the server triggering the bug.

:information_source: [Hoppscotch](https://hoppscotch.io/realtime/) (Realtime => WebSocket => `ws://localhost:8000/client`) is a useful tool to achieve this result


## 6.	Coding Styles

A simple way to have a coherent style across the codebase is to use the IDE features of "code cleanup". For example, in IntelliJ, click on the `src/main/scala` folder and then on `Code -> Reformat Code`. You can then check "include subdirectories", "optimize imports", and "cleanup code" checkbox options. Be careful to not apply these changes to `src/test` folder as it transforms the scalatest syntax into a mess difficult to understand.

Moreover, check that your favorite editor is detecting & using the `.editorconfig` file at the root of the project



## 7. Server `.jar` release

We have installed a simple but powerful plugin ([sbt-assembly](https://github.com/sbt/sbt-assembly)) to help build the "über-jar" version of the project. In the project folder, simply execute

```bash
sbt assembly
```

The all-in-one jar will automatically be created and located at `target/scala-x.xx/pop-assembly-0.x.jar`.

