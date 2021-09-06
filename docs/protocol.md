# Protocol specification

<!-- START doctoc.sh generated TOC please keep comment here to allow auto update -->
<!-- DO NOT EDIT THIS SECTION, INSTEAD RE-RUN doctoc.sh TO UPDATE -->
**:book: Table of Contents**

- [Protocol specification](#protocol-specification)
- [Introduction](#introduction)
  - [Validation and Disambiguation](#validation-and-disambiguation)
  - [Representation of complex data types in the protocol](#representation-of-complex-data-types-in-the-protocol)
  - [Concatenation for hashing](#concatenation-for-hashing)
- [Root message (low-level)](#root-message-low-level)
  - [RPC payload](#rpc-payload)
  - [Query](#query)
    - [Query payload](#query-payload)
    - [Subscribe](#subscribe)
    - [Unsubscribe](#unsubscribe)
    - [Publish](#publish)
      - [Mid-level (publish message) communication](#mid-level-publish-message-communication)
      - [High-level (publish application) messages](#high-level-publish-application-messages)
      - [Creating a LAO](#creating-a-lao)
      - [Update LAO properties](#update-lao-properties)
      - [LAO state broadcast](#lao-state-broadcast)
      - [Witness a message](#witness-a-message)
      - [Creating a Meeting](#creating-a-meeting)
      - [Meeting state broadcast](#meeting-state-broadcast)
      - [Roll Calls (introduction)](#roll-calls-introduction)
      - [Creating a Roll-Call](#creating-a-roll-call)
      - [Opening a Roll-Call](#opening-a-roll-call)
      - [Closing a Roll-Call](#closing-a-roll-call)
      - [Reopening a Roll-Call](#reopening-a-roll-call)
    - [Propagating a message on a channel](#propagating-a-message-on-a-channel)
    - [Catching up on past messages on a channel](#catching-up-on-past-messages-on-a-channel)
  - [Answer](#answer)
    - [RPC answer error](#rpc-answer-error)

<!-- END doctoc.sh generated TOC please keep comment here to allow auto update -->

**Note**: do not edit JSON messages directly. Those are automatically embedded
from `../protocol`. Use [embedme](https://github.com/zakhenry/embedme) to make
an update.

# Introduction

The Personhood.Online system will communicate over WebSockets and rely on a
Publish/Subscribe communication pattern, to support the Personhood.Online notion
of “Communication channels” described in Data Pipeline architecture
specification. As WebSockets do not naturally support Publish/Subscribe, a
low-level protocol is described to provide the Publish/Subscribe communication
layer. Building on top of this low-level protocol, a high-level protocol will
provide the Personhood.Online application-level communication. This protocol is
also described here. The following figure illustrates the principle of a pub/sub
over websocket:

![](assets/pub_sub_principle.png)

## Validation and Disambiguation
To make sure that the protocol is understood by everyone equally and to ensure
that it is implemented correctly, all messages will be described using the JSON
Schema (proposed IETF standard). This will enable the teams to validate their
inputs and outputs with the schema files as part of their testing strategy. The
JSON Schema description is not part of this document and will be provided in a
dedicated branch of the project’s Github repository.

## Representation of complex data types in the protocol

- base64: base64 in string format  
- Public Key: base64  
- Signature: base64  
- Hash: base64  
- Timestamp: uint64 representation of the Unix timestamp (seconds since January
  1st, 1970)  

## Concatenation for hashing

When concatenating strings for hashing, the following logic is applied:

<code>hash(a<sub>1</sub>,a<sub>2</sub>,...,a<sub>n</sub>) = hash(
string(length(a<sub>1</sub>)) || a<sub>1</sub> || string(length(a<sub>2</sub>))
|| a<sub>2</sub> || ... || string(length(a<sub>n</sub>)) ||
a<sub>n</sub>)</code>

where <code>a<sub>1</sub>, ..., a<sub>n</sub></code> are UTF-8 strings,
`length()` computes the length in bytes of the UTF-8 string, `string()` is the
textual representation of a number and `||` represents the concatenation.

# Root message (low-level)

🧭 **RPC Message**

The root message is the basic and lowest definition of an RPC message. It
contains an "RPC payload" that is the different kind of RPC messages exchanged
over the network.

```json5
// ../protocol/genericMessage.json

{
	"$schema": "http://json-schema.org/draft-07/schema#",
	"$id": "https://raw.githubusercontent.com/dedis/student_21_pop/master/protocol/genericMessage.json",
	"title": "Match a custom JsonRpc 2.0 message",
	"description": "Match a client query or a positive or negative server answer.",
	"oneOf": [
		{
			"$ref": "query/query.json"
		},
		{
			"$ref": "answer/answer.json"
		}
	]
}
```

## RPC payload

🧭 **RPC Message** > **RPC payload**

There are two kinds of low-level RPC messages:

- query
- answer

The low-level communication takes care of establishing a channel-based
publish/subscribe mechanism on the WebSocket connection. The implementation of
the low-level communication layer is based on the JSON-RPC 2.0 protocol.

While WebSocket connections function as a bi-directional communication link,
this protocol allows WebSocket clients to subscribe to existing pub/sub channels
(and unsubscribe from them) as well as to publish a message on the pub/sub
channel. It is the responsibility of the organizer & witness servers to
propagate any message received on pub/sub channel C across all open WebSocket
connections listening to the pub/sub channel C.

All low-level, RPC-based communications carry an id field based on the JSON-RPC
2.0 specification. This field is a short-lived, unique id that is used to match
the response with the corresponding request. It is assigned by whoever creates
the request. How this field is generated is up to you, provided that there are
never 2 concurrent requests with the same id in progress on the same WebSocket
connection.

## Query

🧭 **RPC Message** > **RPC payload** (*Query*)

A query denotes data sent over a channel.

```json5
// ../protocol/query/query.json

{
	"$schema": "http://json-schema.org/draft-07/schema#",
	"$id": "https://raw.githubusercontent.com/dedis/student_21_pop/master/protocol/query/query.json",
	"title": "Match a custom JsonRpc 2.0 query message",
	"description": "Match a client query",
	"type": "object",
	"allOf": [
		{
			"maxProperties": 4,
			"$comment": "Note: can't use \"additionalProperties: false\" due to \"allOf\""
		},
		{
			"properties": {
				"jsonrpc": {
					"description": "[String] JsonRpc version",
					"const": "2.0",
					"$comment": "should always be \"2.0\""
				}
			},
			"required": [
				"jsonrpc"
			]
		},
		{
			"oneOf": [
				{
					"$ref": "method/subscribe.json"
				},
				{
					"$ref": "method/unsubscribe.json"
				},
				{
					"$ref": "method/catchup.json"
				},
				{
					"$ref": "method/broadcast.json"
				},
				{
					"$ref": "method/publish.json"
				}
			]
		}
	]
}
```

### Query payload

🧭 **RPC Message** > **RPC payload** (*Query*) > **Query payload**

The query payload denotes the different kind of query messages.

* Subscribe
* Unsubscribe
* Catchup
* Broadcast
* Publish

### Subscribe

🧭 **RPC Message** > **RPC payload** (*Query*) > **Query payload** (*Subscribe*)

By executing a subscribe action, a client can start receiving messages from that
channel.

To simplify the initialization of the system and keep the low-level
communication simple, it is assumed that channel “/root” always exists, and only
the server is allowed to subscribe to it. Clients can then publish on channel
"/root" to create and bootstrap their Local Autonomous Organizer (LAO) (cf
High-level communication).

```json5
// ../protocol/query/method/subscribe.json

{
	"$schema": "http://json-schema.org/draft-07/schema#",
	"$id": "https://raw.githubusercontent.com/dedis/student_21_pop/master/protocol/query/method/subscribe.json",
	"description": "Match subscribe to a channel client query",
	"type": "object",
	"properties": {
		"method": {
			"description": "[String] operation to be performed by the query",
			"const": "subscribe"
		},
		"params": {
			"type": "object",
			"properties": {
				"channel": {
					"description": "[String] name of the channel",
					"$ref": "channel/subChannel.json"
				}
			},
			"additionalProperties": false,
			"required": [
				"channel"
			]
		},
		"id": {
			"type": "integer"
		}
	},
	"required": [
		"method",
		"params",
		"id"
	]
}
```

### Unsubscribe

🧭 **RPC Message** > **RPC payload** (*Query*) > **Query payload**
(*Unsubscribe*)

By executing an unsubscribe action, a client stops receiving messages from that
channel.

```json5
// ../protocol/query/method/unsubscribe.json

{
	"$schema": "http://json-schema.org/draft-07/schema#",
	"$id": "https://raw.githubusercontent.com/dedis/student_21_pop/master/protocol/query/method/unsubscribe.json",
	"description": "Match unsubscribe from a channel client query",
	"type": "object",
	"properties": {
		"method": {
			"description": "[String] operation to be performed by the query",
			"const": "unsubscribe"
		},
		"params": {
			"type": "object",
			"properties": {
				"channel": {
					"description": "[String] name of the channel",
					"$ref": "channel/subChannel.json"
				}
			},
			"additionalProperties": false,
			"required": [
				"channel"
			]
		},
		"id": {
			"type": "integer"
		}
	},
	"required": [
		"method",
		"params",
		"id"
	]
}
```

### Publish

🧭 **RPC Message** > **RPC payload** (*Query*) > **Query payload** (*Publish*)

By executing a publish action, an attendee communicates its intention to publish
a specific message on a channel.

```json5
// ../protocol/query/method/publish.json

{
	"$schema": "http://json-schema.org/draft-07/schema#",
	"$id": "https://raw.githubusercontent.com/dedis/student_21_pop/master/protocol/query/method/publish.json",
	"description": "Match publish query",
	"type": "object",
	"properties": {
		"method": {
			"description": "[String] operation to be performed by the query",
			"const": "publish"
		},
		"params": {
			"type": "object",
			"properties": {
				"channel": {
					"description": "[String] name of the channel",
					"type": "string",
					"pattern": "^\/root(\/[^\/]+)*$",
					"$comment": "Note: the regex matches a \"/root\" or a \"/root/<channel>\""
				},
				"message": {
					"description": "[String] message to be published",
					"type": "object",
					"$comment": "Note: general property declaration to fulfill the additionalProperties requirements"
				}
			},
			"allOf": [
				{
					"if": {
						"properties": {
							"channel": {
								"$ref": "channel/rootChannel.json"
							}
						}
					},
					"then": {
						"properties": {
							"message": {
								"description": "[Message] message to be published",
								"$ref": "message/messageGeneral.json"
							}
						},
						"$comment": "Note: match a publish query destined for the root channel"
					}
				},
				{
					"if": {
						"properties": {
							"channel": {
								"$ref": "channel/subChannel.json"
							}
						}
					},
					"then": {
						"properties": {
							"message": {
								"description": "[Message] message to be published",
								"anyOf": [
									{
										"$ref": "message/messageGeneral.json"
									},
									{
										"$ref": "message/messageWitnessMessage.json"
									}
								]
							}
						},
						"$comment": "Note: match a publish query destined for a sub-channel"
					}
				}
			],
			"additionalProperties": false,
			"required": [
				"channel",
				"message"
			]
		},
		"id": {
			"type": "integer"
		}
	},
	"required": [
		"method",
		"params",
		"id"
	],
	"$comment": "Note: multiple queries have the \"publish\" method"
}
```

#### Mid-level (publish message) communication

🧭 **RPC Message** > **RPC payload** (*Query*) > **Query payload** (*Publish*) >
**Mid level**

Building upon the low-level communication protocol, any communication is
expressed as a message object being sent or received on a channel (cf.
“Publishing a message on a channel”). This includes the creation of a LAO,
changes to LAO properties, creation of meetings, roll-calls, polls, etc. 


As this layer builds on the low-level communication protocol, we already have
operation confirmation / error management logic and don’t need to duplicate it.

**Message object** - The message data specific to the operation is encapsulated
in the data field, whereas the other fields attest to its :

- Origin: `sender` field
- Authenticity: `signature` and `witness_signatures` fields
- Uniqueness: `message_id` field

The `witness_signatures` field will be empty when the message is first sent by
the sender. It will subsequently be filled by the server, as it receives
signatures by witnesses (asynchronously). When a client receives past messages,
the witness signatures will be included. 

As you can see, the `data` field is base64 encoded. This derives from the
necessity to hash and sign the field. An object cannot be signed, but one could
imagine signing its JSON representation. However, a study of the JSON format
will reveal that there's no "canonicalized", unique binary representation of
JSON. That is, different systems may represent the same JSON object in different
ways. The two following structures are identical JSON objects but have very
different binary representations (field order, whitespace, newlines, unicode,
etc.):

```
{ "text": "message", "temp": "15\u00f8C" }
```

VS

```
{"temp":"15°C","text":"message"}
```

As a consequence, we decide to encode the sender's preferred representation (any
valid representation) in base64, and then use that representation as input to
our hash or sign function. A nice side effect of this design is that the message
object signature verification is entirely independent from the structure of the
data field itself: no matter what message you send, the verification will always
work in the same way (compare this to signing the data field-by-field!). Once
the `data` field is unencoded and parsed, the receiving peer can simply validate
it and process it at the application level.

```json5
// ../protocol/query/method/message/messageGeneral.json

{
	"$schema": "http://json-schema.org/draft-07/schema#",
	"$id": "https://raw.githubusercontent.com/dedis/student_21_pop/master/protocol/query/method/message/messageGeneral.json",
	"title": "This represents a message someone would want to publish on a specific channel",
	"description": "Match general message content (Create LAO, Update LAO, Broadcast LAO, Create Meeting, Broadcast Meeting)",
	"type": "object",
	"properties": {
		"data": {
			"description": "[Base64String] data contained in the message",
			"type": "string",
			"contentEncoding": "base64",
			"$comment": "Note: the string is encoded in Base64"
		},
		"sender": {
			"description": "[Base64String] public key of the sender/organizer/server",
			"type": "string",
			"contentEncoding": "base64",
			"$comment": "Note: the string is encoded in Base64"
		},
		"signature": {
			"description": "[Base64String] organizer's signature on data : Sign(data)",
			"type": "string",
			"contentEncoding": "base64",
			"$comment": "Note: the string is encoded in Base64"
		},
		"message_id": {
			"description": "[Base64String] message id : Hash(data||signature)",
			"type": "string",
			"contentEncoding": "base64",
			"$comment": "Note: the string is encoded in Base64"
		},
		"witness_signatures": {
			"description": "[Array[Base64String]] signatures of the witnesses on the modification message (either creation/update)",
			"type": "array",
			"items": {
				"type": "object",
				"properties": {
					"witness": {
						"description": "[Base64String] public key of the witness",
						"type": "string",
						"contentEncoding": "base64",
						"$comment": "Note: the string is encoded in Base64"
					},
					"signature": {
						"description": "[Base64String] witness' signature : Sign(message_id)",
						"type": "string",
						"contentEncoding": "base64",
						"$comment": "Note: the strings are encoded in Base64"
					}
				},
				"additionalProperties": false,
				"required": [
					"witness",
					"signature"
				]
			},
			"$comment": "Note: the items are encoded in Base64"
		}
	},
	"additionalProperties": false,
	"required": [
		"data",
		"sender",
		"signature",
		"message_id",
		"witness_signatures"
	]
}
```

#### High-level (publish application) messages

🧭 **RPC Message** > **RPC payload** (*Query*) > **Query payload** (*Publish*) >
**Mid level** > **High level**

The publish query can contain the following object#action messages:

* lao#create
* lao#update_properties
* lao#state
* message#witness
* meeting#create
* meeting#state
* roll_call#create
* roll_call#open
* roll_call#close
* roll_call#reopen

#### Creating a LAO

**RPC Message** > **RPC payload** (*Query*) > **Query payload** (*Publish*) >
**Mid Level** > **High level** (*lao#create*)

By sending the lao/create message to the organizer’s server’s default channel
(“/root”), the main channel of the LAO will be created with the identifier id.
At that point, any of the clients, including the organizer, can subscribe to the
channel id and send messages over it. The server is expected to verify the data,
such that the last modified timestamp is equal to the creation timestamp, that
the timestamp is reasonably recent with respect to the server’s clock, that the
attestation is valid, etc. Upon successful creation, the organizer is expected
to broadcast the LAO state to all witnesses and clients (see “LAO state
broadcast”).

```json5
// ../protocol/query/method/message/data/dataCreateLao.json

{
	"$schema": "http://json-schema.org/draft-07/schema#",
	"$id": "https://raw.githubusercontent.com/dedis/student_21_pop/master/protocol/query/method/message/data/dataCreateLao.json",
	"description": "Match a create LAO query",
	"type": "object",
	"properties": {
		"object": {
			"const": "lao"
		},
		"action": {
			"const": "create"
		},
		"id": {
			"type": "string",
			"contentEncoding": "base64",
			"$comment": "Hash : SHA256(organizer||creation||name)"
		},
		"name": {
			"type": "string"
		},
		"creation": {
			"description": "[Timestamp] creation time",
			"type": "integer",
			"minimum": 0
		},
		"organizer": {
			"description": "[Base64String] public key of the organizer",
			"type": "string",
			"contentEncoding": "base64"
		},
		"witnesses": {
			"description": "[Array[Base64String]] list of public keys of witnesses",
			"type": "array",
			"uniqueItems": true,
			"items": {
				"type": "string",
				"contentEncoding": "base64"
			}
		}
	},
	"additionalProperties": false,
	"required": [
		"object",
		"action",
		"id",
		"name",
		"creation",
		"organizer",
		"witnesses"
	],
	"note": [
		"By sending the lao/create message to the organizer’s server’s default channel (\"/root\"), the main channel of ",
		"the LAO will be created with the identifier id. At that point, any of the clients, including the organizer, can ",
		"subscribe to the channel id and send messages over it. The server is expected to verify the data, such that the ",
		"last modified timestamp is equal to the creation timestamp, that the timestamp is reasonably recent with respect ",
		"to the server’s clock, that the attestation is valid, etc. Upon successful creation, the organizer is ",
		"expected to broadcast the LAO state to all witnesses and clients (see \"LAO state broadcast\")"
	]
}
```

#### Update LAO properties

🧭 **RPC Message** > **RPC payload** (*Query*) > **Query payload** (*Publish*) >
**Mid Level** > **High level** (*lao#update_properties*)

By sending the lao/update_properties message to the LAO’s main channel (LAO's
“id”), the LAO name, list of witnesses and last modified timestamp are updated.
The server is expected to forward the message to the witnesses and clients. The
server is expected to verify the data, including timestamp freshness. Upon
successful modification, the organizer is expected to broadcast the LAO state to
all witnesses and clients (see “LAO state broadcast”).


```json5
// ../protocol/query/method/message/data/dataUpdateLao.json

{
	"$schema": "http://json-schema.org/draft-07/schema#",
	"$id": "https://raw.githubusercontent.com/dedis/student_21_pop/master/protocol/query/method/message/data/dataUpdateLao.json",
	"description": "Match an update LAO query",
	"type": "object",
	"properties": {
		"object": {
			"const": "lao"
		},
		"action": {
			"const": "update_properties"
		},
		"id": {
			"type": "string",
			"contentEncoding": "base64",
			"$comment": "Hash : SHA256(organizer||creation||name)"
		},
		"name": {
			"type": "string"
		},
		"last_modified": {
			"description": "[Timestamp] last modification's time",
			"type": "integer",
			"minimum": 0
		},
		"witnesses": {
			"description": "[Array[Base64String]] list of public keys of witnesses",
			"type": "array",
			"uniqueItems": true,
			"items": {
				"type": "string",
				"contentEncoding": "base64"
			}
		}
	},
	"additionalProperties": false,
	"required": [
		"object",
		"action",
		"id",
		"name",
		"last_modified",
		"witnesses"
	],
	"note": [
		"By sending the lao/update_properties message to the LAO’s main channel (LAO's id), the LAO name, list of ",
		"witnesses and last modified timestamp are updated. The server is expected to forward the message to the ",
		"witnesses and clients. The server is expected to verify the data, including timestamp freshness. Upon successful ",
		"modification, the organizer is expected to broadcast the LAO state to all witnesses and clients (see ",
		"\"LAO state broadcast\")"
	]
}
```

#### LAO state broadcast

🧭 **RPC Message** > **RPC payload** (*Query*) > **Query payload** (*Publish*) >
**Mid Level** > **High level** (*lao#state*)

When a LAO is created or modified, the organizer is expected to publish the
lao/state message to the LAO’s main channel (LAO's “id”), once it has received
the required number of witness signatures.

```json5
// ../protocol/query/method/message/data/dataStateLao.json

{
	"$schema": "http://json-schema.org/draft-07/schema#",
	"$id": "https://raw.githubusercontent.com/dedis/student_21_pop/master/protocol/query/method/message/data/dataStateLao.json",
	"description": "Match a state broadcast LAO query",
	"type": "object",
	"properties": {
		"object": {
			"const": "lao"
		},
		"action": {
			"const": "state"
		},
		"id": {
			"type": "string",
			"contentEncoding": "base64",
			"$comment": "Hash : SHA256(organizer||creation||name)"
		},
		"name": {
			"type": "string"
		},
		"creation": {
			"description": "[Timestamp] creation time",
			"type": "integer",
			"minimum": 0
		},
		"last_modified": {
			"description": "[Timestamp] last modification's time",
			"type": "integer",
			"minimum": 0
		},
		"organizer": {
			"description": "[Base64String] public key of the organizer",
			"type": "string",
			"contentEncoding": "base64"
		},
		"witnesses": {
			"description": "[Array[Base64String]] list of public keys of witnesses",
			"type": "array",
			"uniqueItems": true,
			"items": {
				"type": "string",
				"contentEncoding": "base64"
			}
		},
		"modification_id": {
			"description": "[Base64String] id of the modification (either creation/update)",
			"type": "string",
			"contentEncoding": "base64"
		},
		"modification_signatures": {
			"description": "[Array[Base64String]] signatures of the witnesses on the modification message (either creation/update)",
			"type": "array",
			"items": {
				"type": "object",
				"properties": {
					"witness": {
						"description": "[Base64String] public key of the witness",
						"type": "string",
						"contentEncoding": "base64",
						"$comment": "Note: the string is encoded in Base64"
					},
					"signature": {
						"description": "[Base64String] witness' signature : Sign(message_id)",
						"type": "string",
						"contentEncoding": "base64",
						"$comment": "Note: the strings are encoded in Base64"
					}
				},
				"additionalProperties": false,
				"required": [
					"witness",
					"signature"
				]
			}
		}
	},
	"additionalProperties": false,
	"required": [
		"object",
		"action",
		"id",
		"name",
		"creation",
		"last_modified",
		"organizer",
		"witnesses",
		"modification_id",
		"modification_signatures"
	],
	"note": [
		"When a LAO is created or modified, the organizer is expected to publish the lao/state message to the ",
		"LAO’s main channel (LAO's id), once it got the required number of witness signatures"
	]
}
```

#### Witness a message

🧭 **RPC Message** > **RPC payload** (*Query*) > **Query payload** (*Publish*) >
**Mid Level** > **High level** (*message#witness*)

By sending the message/witness message to the LAO’s main channel (LAO's “id”), a
witness can attest to the message. Upon reception, the server and the witnesses
add this signature to the existing message’s `witness_signatures` field. When a
new client retrieves this message, the `witness_signatures` field will be
populated with all the witness signatures received by the server.

```json5
// ../protocol/query/method/message/data/dataWitnessMessage.json

{
	"$schema": "http://json-schema.org/draft-07/schema#",
	"$id": "https://raw.githubusercontent.com/dedis/student_21_pop/master/protocol/query/method/message/data/dataWitnessMessage.json",
	"description": "Match a witness a message query",
	"type": "object",
	"properties": {
		"object": {
			"const": "message"
		},
		"action": {
			"const": "witness"
		},
		"message_id": {
			"type": "string",
			"contentEncoding": "base64",
			"note": "message_id of the message to witness"
		},
		"signature": {
			"description": "[Base64String] signature by the witness over the \"message_id\" field of the message",
			"type": "string",
			"contentEncoding": "base64"
		}
	},
	"additionalProperties": false,
	"required": [
		"object",
		"action",
		"message_id",
		"signature"
	],
	"note": [
		"By sending the message/witness message to the LAO’s main channel (LAO's id), a witness can attest to the ",
		"message. Upon reception, the server and the witnesses add this signature to the existing message’s ",
		"witness_signatures field. When a new client retrieves this message, the witness_signatures field will be ",
		"populated with all the witness signatures received by the server"
	]
}
```

#### Creating a Meeting

🧭 **RPC Message** > **RPC payload** (*Query*) > **Query payload** (*Publish*) >
**Mid Level** > **High level** (*meeting#create*)

By sending the meeting/create message to the LAO’s main channel (LAO's “id”),
the meeting will be created with the identifier `id`. The server is expected to
verify the data, such that the last modified timestamp is equal to the creation
timestamp, that the timestamp is reasonably recent with respect to the server’s
clock, that the attestation is valid, etc. Upon successful creation, the server
is expected to broadcast the Meeting state to all witnesses and clients (see
“Meeting state broadcast”).

```json5
// ../protocol/query/method/message/data/dataCreateMeeting.json

{
	"$schema": "http://json-schema.org/draft-07/schema#",
	"$id": "https://raw.githubusercontent.com/dedis/student_21_pop/master/protocol/query/method/message/data/dataCreateMeeting.json",
	"description": "Match a create Meeting query",
	"type": "object",
	"properties": {
		"object": {
			"const": "meeting"
		},
		"action": {
			"const": "create"
		},
		"id": {
			"type": "string",
			"contentEncoding": "base64",
			"$comment": "Hash : SHA256('M'||lao_id||creation||name)"
		},
		"name": {
			"type": "string"
		},
		"creation": {
			"description": "[Timestamp] creation time",
			"type": "integer",
			"minimum": 0
		},
		"location": {
			"description": "[String] location of the meeting",
			"type": "string",
			"$comment": "Note: optional"
		},
		"start": {
			"description": "[Timestamp] start time",
			"type": "integer",
			"minimum": 0
		},
		"end": {
			"description": "[Timestamp] end time",
			"type": "integer",
			"minimum": 0,
			"$comment": "Note: optional"
		},
		"extra": {
			"description": "[JsObject] arbitrary object for extra information",
			"type": "object",
			"$comment": "Note: optional"
		}
	},
	"additionalProperties": false,
	"required": [
		"object",
		"action",
		"id",
		"name",
		"creation",
		"start"
	],
	"note": [
		"By sending the meeting/create message to the LAO’s main channel (LAO's id), the meeting will be created with ",
		"the identifier id. The server is expected to verify the data, such that the last modified timestamp is equal ",
		"to the creation timestamp, that the timestamp is reasonably recent with respect to the server’s clock, that ",
		"the attestation is valid, etc. Upon successful creation, the organizer is expected to broadcast the Meeting state ",
		"to all witnesses and clients (see \"Meeting state broadcast\")"
	]
}
```

#### Meeting state broadcast

🧭 **RPC Message** > **RPC payload** (*Query*) > **Query payload** (*Publish*) >
**Mid Level** > **High level** (*meeting#state*)

When a meeting is created, modified or attested to by a witness, the server is
expected to publish the meeting/state message to the LAO’s main channel (LAO's
“id”). 

```json5
// ../protocol/query/method/message/data/dataStateMeeting.json

{
	"$schema": "http://json-schema.org/draft-07/schema#",
	"$id": "https://raw.githubusercontent.com/dedis/student_21_pop/master/protocol/query/method/message/data/dataStateMeeting.json",
	"description": "Match a state broadcast Meeting query",
	"type": "object",
	"properties": {
		"object": {
			"const": "meeting"
		},
		"action": {
			"const": "state"
		},
		"id": {
			"type": "string",
			"contentEncoding": "base64",
			"$comment": "Hash : SHA256('M'||lao_id||creation||name)"
		},
		"name": {
			"type": "string"
		},
		"creation": {
			"description": "[Timestamp] creation time",
			"type": "integer",
			"minimum": 0
		},
		"last_modified": {
			"description": "[Timestamp] last modification's time",
			"type": "integer",
			"minimum": 0
		},
		"location": {
			"description": "[String] location of the meeting",
			"type": "string",
			"$comment": "Note: optional"
		},
		"start": {
			"description": "[Timestamp] start time",
			"type": "integer",
			"minimum": 0
		},
		"end": {
			"description": "[Timestamp] end time",
			"type": "integer",
			"minimum": 0,
			"$comment": "Note: optional"
		},
		"extra": {
			"description": "[JsObject] arbitrary object for extra information",
			"type": "object",
			"$comment": "Note: optional"
		},
		"modification_id": {
			"description": "[Base64String] id of the modification (either creation/update)",
			"type": "string",
			"contentEncoding": "base64"
		},
		"modification_signatures": {
			"description": "[Array[Base64String]] signatures of the witnesses on the modification message (either creation/update)",
			"type": "array",
			"items": {
				"type": "object",
				"properties": {
					"witness": {
						"description": "[Base64String] public key of the witness",
						"type": "string",
						"contentEncoding": "base64",
						"$comment": "Note: the string is encoded in Base64"
					},
					"signature": {
						"description": "[Base64String] witness' signature : Sign(message_id)",
						"type": "string",
						"contentEncoding": "base64",
						"$comment": "Note: the strings are encoded in Base64"
					}
				},
				"additionalProperties": false,
				"required": [
					"witness",
					"signature"
				]
			}
		}
	},
	"additionalProperties": false,
	"required": [
		"object",
		"action",
		"id",
		"name",
		"creation",
		"last_modified",
		"start",
		"modification_id",
		"modification_signatures"
	],
	"note": [
		"When a meeting is created or modified, the organizer is expected to publish the ",
		"meeting/state message to the LAO’s main channel (LAO's id)"
	]
}
```

#### Roll Calls (introduction)

A roll call has the following state transitions:

Created → Opened → Closed → Reopened → Closed

**Created**: This states denotes the organizers intention of organizing a new
roll call.  
**Opened**: This state denotes that the roll call is open and the organizer and
**witnesses** are ready to scan the QR code(s) of the attendees.  
**Closed**: This state denotes the closing of a roll call and contains all the
**attendee** public keys scanned during the roll call process.  

#### Creating a Roll-Call

🧭 **RPC Message** > **RPC payload** (*Query*) > **Query payload** (*Publish*) >
**Mid Level** > **High level** (*roll_call#create*)

An organizer creates a roll-call by publishing a roll_call/create message to the
LAO’s channel. The proposed_start and proposed_end fields denote the start and
end time for the roll call. Note that these fields are only used to position the
event correctly in the list of LAO events. Witnesses witness the roll call event
ensuring that each attendee is scanned exactly once.

```json5
// ../protocol/query/method/message/data/dataCreateRollCall.json

{
	"$schema": "http://json-schema.org/draft-07/schema#",
	"$id": "https://raw.githubusercontent.com/dedis/student_21_pop/master/protocol/query/method/message/data/dataCreateRollCall.json",
	"description": "Match a create roll-call query",
	"type": "object",
	"properties": {
		"object": {
			"const": "roll_call"
		},
		"action": {
			"const": "create"
		},
		"id": {
			"type": "string",
			"contentEncoding": "base64",
			"$comment": "Hash : SHA256('R'||lao_id||creation||name)"
		},
		"name": {
			"type": "string"
		},
		"creation": {
			"description": "[Timestamp] creation time",
			"type": "integer",
			"minimum": 0
		},
		"proposed_start": {
			"description": "[Timestamp] proposed start time",
			"type": "integer",
			"minimum": 0
		},
		"proposed_end": {
			"description": "[Timestamp] proposed end time",
			"type": "integer",
			"minimum": 0
		},
		"location": {
			"description": "[String] location of the roll-call",
			"type": "string"
		},
		"description": {
			"description": "An optional description of the meeting",
			"type": "string",
			"$comment": "Optional"
		}
	},
	"additionalProperties": false,
	"required": [
		"object",
		"action",
		"id",
		"name",
		"creation",
		"location",
		"proposed_start",
		"proposed_end"
	],
	"note": [
		"The roll_call/create message denotes the organizer's intention to create a new roll call",
		"The proposed_start and proposed_end fields denote the start and end time for the roll call for displaying the event in the UI.",
		"The roll call may however be only considered started/closed based on its latest state and the attestation of witnesses for the message leading to that state transition."
	]
}
```

#### Opening a Roll-Call

🧭 **RPC Message** > **RPC payload** (*Query*) > **Query payload** (*Publish*) >
**Mid Level** > **High level** (*roll_call#open*)

A roll-call may be opened by the organizer by publishing a roll_call/open
message on the LAO channel.

```json5
// ../protocol/query/method/message/data/dataOpenRollCall.json

{
	"$schema": "http://json-schema.org/draft-07/schema#",
	"$id": "https://raw.githubusercontent.com/dedis/student_21_pop/master/protocol/query/method/message/data/dataOpenRollCall.json",
	"description": "Match a open roll-call query",
	"type": "object",
	"properties": {
		"object": {
			"const": "roll_call"
		},
		"action": {
			"enum": [
				"open",
				"reopen"
			]
		},
		"update_id": {
			"type": "string",
			"contentEncoding": "base64",
			"$comment": "Hash : SHA256('R'||lao_id||opens||opened_at)"
		},
		"opens": {
			"type": "string",
			"contentEncoding": "base64",
			"$comment": "The 'update_id' of the latest roll call close, or in its absence, the 'id' field of the roll call creation"
		},
		"opened_at": {
			"description": "[Timestamp] start time",
			"type": "integer",
			"minimum": 0,
			"$comment": "The time at which the roll call is opened/reopened"
		}
	},
	"additionalProperties": false,
	"required": [
		"object",
		"action",
		"update_id",
		"opens",
		"opened_at"
	],
	"note": [
		"The roll_call/open message opens a roll call. This message would lead to a transition from the",
		"created -> opened state after witnesses attest the message. Witnesses should attest this when they",
		"can ensure the roll call has actually started. roll_call/reopen may be used to re-open a",
		"closed roll call in case of a human error (forgot to scan a QR Code). A roll_call/reopen attested by",
		"witnesses would cause a transition from closed -> opened state"
	]
}
```

#### Closing a Roll-Call

🧭 **RPC Message** > **RPC payload** (*Query*) > **Query payload** (*Publish*) >
**Mid Level** > **High level** (*roll_call#close*)

A roll-call may be closed by the organizer by publishing a roll_call/close
message on the LAO channel. This is effectively the message that will be sent by
the organizer after scanning all attendees’ public key. 

![](assets/close_roll_call.png)

```json5
// ../protocol/query/method/message/data/dataCloseRollCall.json

{
	"$schema": "http://json-schema.org/draft-07/schema#",
	"$id": "https://raw.githubusercontent.com/dedis/student_21_pop/master/protocol/query/method/message/data/dataCloseRollCall.json",
	"description": "Match a close roll-call query",
	"type": "object",
	"properties": {
		"object": {
			"const": "roll_call"
		},
		"action": {
			"const": "close"
		},
		"update_id": {
			"type": "string",
			"contentEncoding": "base64",
			"$comment": "Hash : SHA256('R'||lao_id||closes||closed_at)"
		},
		"closes": {
			"type": "string",
			"contentEncoding": "base64",
			"$comment": "The 'update_id' of the latest roll call open"
		},
		"closed_at": {
			"description": "[Timestamp] end time",
			"type": "integer",
			"minimum": 0
		},
		"attendees": {
			"description": "[Array[Base64String]] list of public keys of attendees",
			"type": "array",
			"uniqueItems": true,
			"items": {
				"type": "string",
				"contentEncoding": "base64"
			}
		}
	},
	"additionalProperties": false,
	"required": [
		"object",
		"action",
		"update_id",
		"closes",
		"closed_at",
		"attendees"
	],
	"note": [
		"When the organizer scanned all the public keys of the attendees, it closes the event",
		"and broadcast attendees public keys. A roll_call/close message attested by witnesses",
		"leads to a transition from opened -> closed state. Usually, the event can only be closed once.",
		"In some special case, the event may be reopened (e.g. the organizer forgot to scan the key",
		"of an attendee, so we reopen 2 minutes later). In this case, we can close the event a",
		"second (or more) time. Please see `dataOpenRollCall.json` for more information."
	]
}
```

#### Reopening a Roll-Call

🧭 **RPC Message** > **RPC payload** (*Query*) > **Query payload** (*Publish*) >
**Mid Level** > **High level** (*roll_call#reopen*)

A closed roll-call may be re-opened by the organizer by publishing a
roll_call/reopen message on the LAO channel. This is useful in scenarios where
the organizer forgets to scan an attendee’s public key.

![](assets/reopen_roll_call.png)

```json5
// ../protocol/query/method/message/data/dataOpenRollCall.json

{
	"$schema": "http://json-schema.org/draft-07/schema#",
	"$id": "https://raw.githubusercontent.com/dedis/student_21_pop/master/protocol/query/method/message/data/dataOpenRollCall.json",
	"description": "Match a open roll-call query",
	"type": "object",
	"properties": {
		"object": {
			"const": "roll_call"
		},
		"action": {
			"enum": [
				"open",
				"reopen"
			]
		},
		"update_id": {
			"type": "string",
			"contentEncoding": "base64",
			"$comment": "Hash : SHA256('R'||lao_id||opens||opened_at)"
		},
		"opens": {
			"type": "string",
			"contentEncoding": "base64",
			"$comment": "The 'update_id' of the latest roll call close, or in its absence, the 'id' field of the roll call creation"
		},
		"opened_at": {
			"description": "[Timestamp] start time",
			"type": "integer",
			"minimum": 0,
			"$comment": "The time at which the roll call is opened/reopened"
		}
	},
	"additionalProperties": false,
	"required": [
		"object",
		"action",
		"update_id",
		"opens",
		"opened_at"
	],
	"note": [
		"The roll_call/open message opens a roll call. This message would lead to a transition from the",
		"created -> opened state after witnesses attest the message. Witnesses should attest this when they",
		"can ensure the roll call has actually started. roll_call/reopen may be used to re-open a",
		"closed roll call in case of a human error (forgot to scan a QR Code). A roll_call/reopen attested by",
		"witnesses would cause a transition from closed -> opened state"
	]
}
```

### Propagating a message on a channel

🧭 **RPC Message** > **RPC payload** (*Query*) > **Query payload** (*Broadcast*)

To broadcast a message that was published on a given channel, the server sends
out a JSON-RPC 2.0 notification as defined below. Do notice the absence of an id
field and of a response, in compliance with the JSON-RPC 2.0 specification.

```json5
// ../protocol/query/method/broadcast.json

{
	"$schema": "http://json-schema.org/draft-07/schema#",
	"$id": "https://raw.githubusercontent.com/dedis/student_21_pop/master/protocol/query/method/broadcast.json",
	"description": "Match propagation/broadcast of a message on a channel query",
	"type": "object",
	"properties": {
		"method": {
			"description": "[String] operation to be performed by the query",
			"const": "broadcast"
		},
		"params": {
			"type": "object",
			"properties": {
				"channel": {
					"description": "[String] name of the channel",
					"$ref": "channel/subChannel.json"
				},
				"message": {
					"description": "[Message] message to be published",
					"anyOf": [
						{
							"$ref": "message/messageGeneral.json"
						},
						{
							"$ref": "message/messageWitnessMessage.json"
						}
					]
				}
			},
			"additionalProperties": false,
			"required": [
				"channel",
				"message"
			]
		}
	},
	"maxProperties": 3,
	"required": [
		"method",
		"params"
	],
	"$comment": "Note: only 3 properties on this query!"
}
```

### Catching up on past messages on a channel

🧭 **RPC Message** > **RPC payload** (*Query*) > **Query payload** (*Catchup*)

By executing a catchup action, a client can ask the server to receive all past
messages on a specific channel.

```json5
// ../protocol/query/method/catchup.json

{
	"$schema": "http://json-schema.org/draft-07/schema#",
	"$id": "https://raw.githubusercontent.com/dedis/student_21_pop/master/protocol/query/method/catchup.json",
	"description": "Match catchup on past message on a channel query",
	"type": "object",
	"properties": {
		"method": {
			"description": "[String] operation to be performed by the query",
			"const": "catchup"
		},
		"params": {
			"type": "object",
			"properties": {
				"channel": {
					"description": "[String] name of the channel",
					"$ref": "channel/subChannel.json"
				}
			},
			"additionalProperties": false,
			"required": [
				"channel"
			]
		},
		"id": {
			"type": "integer"
		}
	},
	"required": [
		"method",
		"params",
		"id"
	]
}
```

## Answer

🧭 **RPC Message** > **RPC payload** (*Answer*)

```json5
// ../protocol/answer/answer.json

{
	"$schema": "http://json-schema.org/draft-07/schema#",
	"$id": "https://raw.githubusercontent.com/dedis/student_21_pop/master/protocol/answer/answer.json",
	"title": "Match a custom JsonRpc 2.0 message answer",
	"description": "Match a positive or negative server answer",
	"type": "object",

	"properties": {
		"jsonrpc": { "description": "[String] JsonRpc version", "const": "2.0", "$comment": "should always be \"2.0\"" },
		"result": { 
			"description": "In case of positive answer, result of the client query",
			"oneOf": [
				{ "$ref": "positiveAnswer/positiveAnswerGeneral.json" },
				{ "$ref": "positiveAnswer/positiveAnswerCatchup.json" }
			],
			"$comment": "Note: this field is absent if the answer is negative"
		},
		"error": {
			"description": "In case of negative answer, error generated by the client query",
			"$ref": "negativeAnswer/error.json"
		},
		"id": {
			"oneOf": [
				{ "type": "integer" },
				{ "type": "null" }
			],
			"$comment": "The id match the request id. If there was an error in detecting the id, it must be null"
		      }
	},
	"additionalProperties": false,
	"allOf": [
		{ "required": ["jsonrpc", "id"] },
		{ 
			"oneOf": [
				{ "required": ["result"] },
				{ "required": ["error"] }
			] 
		}
	],
	"$comment": "Note: required fields: (\"jsonrpc\" \\and (\"result\" \\or \"error\") \\and \"id\")"
}

```

### RPC answer error

🧭 **RPC Message** > **RPC payload** (*Answer*) > **Error**

* positiveAnswer
* negativeAnswer