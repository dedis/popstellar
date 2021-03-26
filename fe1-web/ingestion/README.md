# Ingestion logic

This folder contains all the logic to bring data "into" the application from the network.

The setup (in `Configure.ts`) wires two things up:
1. It connects messages received through JSON-RPC with the Redux store (MessageReducer).
   This is a very lightweight process and it happens in `Handler.ts`
   It only processes synchronously 'LAO/Create' messages,
   as those messages are received during catchup and used to detect a new connection to a LAO.

2. It creates a "listener" on the MessageReducer store that parses message as they come in.
   The watcher is setup in `Watcher.ts`,
   but most of the logic is contained in the `./handlers` folder.

This architecture achieves two separate and equally desirable goals:

1. The decoupling of network activity from data processing operations,
   as these two tasks might have very different requirements.

2. The buffering of messages until they can be processed,
   thus supporting out-of-order message processing.

