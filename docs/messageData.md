# High-level ("Message data") messages

<!-- START doctoc.sh generated TOC please keep comment here to allow auto update -->
<!-- DO NOT EDIT THIS SECTION, INSTEAD RE-RUN doctoc.sh TO UPDATE -->
**:book: Table of Contents**

- [](#)

<!-- END doctoc.sh generated TOC please keep comment here to allow auto update -->

**Note**: do not edit JSON messages directly. Those are automatically embedded
from `../protocol`. Use [embedme](https://github.com/zakhenry/embedme) to make
an update.

# Introduction

**RPC Message** > **Query** > **Publish** > **Message** > **Message data**

A `Message` (see [protocol.md](protocol.md)) contains a special `data` field.
This field contains a serialized `Message data` message. A `Message data`
targets an `Object` and an `Action`. The `Object` can be seen as a class, and
the `Action` as a method. We represent this tuple (Object, Action) as a string
`<Object>#<Action>`.

Here are the existing `Message data`, identified by their unique
`<Object>#<Action>` attributes:

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
* post#user_post
* post#server_post
* post#user_remove
* post#server_remove

# LAO

## Creating a LAO (lao#create)

By sending the lao/create message to the organizer’s server’s default channel
(“/root”), the main channel of the LAO will be created with the identifier id.
At that point, any of the clients, including the organizer, can subscribe to the
channel id and send messages over it. The server is expected to verify the data,
such that the last modified timestamp is equal to the creation timestamp, that
the timestamp is reasonably recent with respect to the server’s clock, that the
attestation is valid, etc. Upon successful creation, the organizer is expected
to broadcast the LAO state to all witnesses and clients (see “LAO state
broadcast”).

<details>
<summary>
💡 See an example
</summary>

```json5
// ../protocol/examples/messageData/lao_create.json

{
    "object": "lao",
    "action": "create",
    "id": "XXX",
    "name": "XXX",
    "creation": 123,
    "organizer": "XXX",
    "witnesses": ["XXX"]
}

```

</details>


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

## Update LAO properties (lao#update_properties)

By sending the lao/update_properties message to the LAO’s main channel (LAO's
“id”), the LAO name, list of witnesses and last modified timestamp are updated.
The server is expected to forward the message to the witnesses and clients. The
server is expected to verify the data, including timestamp freshness. Upon
successful modification, the organizer is expected to broadcast the LAO state to
all witnesses and clients (see “LAO state broadcast”).

<details>
<summary>
💡 See an example
</summary>

```json5
// ../protocol/examples/messageData/lao_update.json

{
    "object": "lao",
    "action": "update_properties",
    "id": "XXX",
    "name": "XXX",
    "last_modified": 123,
    "witnesses": ["XXX"]
}

```

</details>

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

## LAO state broadcast (lao#state)

When a LAO is created or modified, the organizer is expected to publish the
lao/state message to the LAO’s main channel (LAO's “id”), once it has received
the required number of witness signatures.

<details>
<summary>
💡 See an example
</summary>

```json5
// ../protocol/examples/messageData/lao_state.json

{
    "object": "lao",
    "action": "state",
    "id": "XXX",
    "name": "XXX",
    "creation": 123,
    "last_modified": 123,
    "organizer": "XXX",
    "witnesses": ["XXX"],
    "modification_id": "XXX",
    "modification_signatures": [
        {
            "witness": "XXX",
            "signature": "XXX"
        }
    ]
}

```

</details>

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
                "required": ["witness", "signature"]
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

# Message

## Witness a message (message#witness)

By sending the message/witness message to the LAO’s main channel (LAO's “id”), a
witness can attest to the message. Upon reception, the server and the witnesses
add this signature to the existing message’s `witness_signatures` field. When a
new client retrieves this message, the `witness_signatures` field will be
populated with all the witness signatures received by the server.

<details>
<summary>
💡 See an example
</summary>

```json5
// ../protocol/examples/messageData/message_witness.json

{
    "object": "message",
    "action": "witness",
    "message_id": "XXX",
    "signature": "XXX"
}

```

</details>

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
    "required": ["object", "action", "message_id", "signature"],
    "note": [
        "By sending the message/witness message to the LAO’s main channel (LAO's id), a witness can attest to the ",
        "message. Upon reception, the server and the witnesses add this signature to the existing message’s ",
        "witness_signatures field. When a new client retrieves this message, the witness_signatures field will be ",
        "populated with all the witness signatures received by the server"
    ]
}

```

# Meeting

## Creating a Meeting (meeting#create)

🧭 **RPC Message** > **RPC payload** (*Query*) > **Query payload** (*Publish*) >
**Mid Level** > **High level** (*meeting#create*)

By sending the meeting/create message to the LAO’s main channel (LAO's “id”),
the meeting will be created with the identifier `id`. The server is expected to
verify the data, such that the last modified timestamp is equal to the creation
timestamp, that the timestamp is reasonably recent with respect to the server’s
clock, that the attestation is valid, etc. Upon successful creation, the server
is expected to broadcast the Meeting state to all witnesses and clients (see
“Meeting state broadcast”).

<details>
<summary>
💡 See an example
</summary>

```json5
// ../protocol/examples/messageData/meeting_create.json

{
    "object": "meeting",
    "action": "create",
    "id": "XXX",
    "name": "XXX",
    "creation": 123,
    "location": "XXX",
    "start": 123,
    "end": 123,
    "extra": { "anything": "XXX" }
}

```

</details>

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
    "required": ["object", "action", "id", "name", "creation", "start"],
    "note": [
        "By sending the meeting/create message to the LAO’s main channel (LAO's id), the meeting will be created with ",
        "the identifier id. The server is expected to verify the data, such that the last modified timestamp is equal ",
        "to the creation timestamp, that the timestamp is reasonably recent with respect to the server’s clock, that ",
        "the attestation is valid, etc. Upon successful creation, the organizer is expected to broadcast the Meeting state ",
        "to all witnesses and clients (see \"Meeting state broadcast\")"
    ]
}

```

## Meeting state broadcast (meeting#state)

When a meeting is created, modified or attested to by a witness, the server is
expected to publish the meeting/state message to the LAO’s main channel (LAO's
“id”). 

<details>
<summary>
💡 See an example
</summary>

```json5
// ../protocol/examples/messageData/meeting_state.json

{
    "object": "meeting",
    "action": "state",
    "id": "XXX",
    "name": "XXX",
    "creation": 123,
    "last_modified": 123,
    "location": "XXX",
    "start": 123,
    "end": 123,
    "extra": {
        "anything": "XXX"
    },
    "modification_id": "XXX",
    "modification_signatures": [
        {
            "witness": "XXX",
            "signature": "XXX"
        }
    ]
}

```

</details>

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
                "required": ["witness", "signature"]
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

# Roll Calls (introduction)

A roll call has the following state transitions:

Created → Opened → Closed → Reopened → Closed

**Created**: This states denotes the organizers intention of organizing a new
roll call.  
**Opened**: This state denotes that the roll call is open and the organizer and
**witnesses** are ready to scan the QR code(s) of the attendees.  
**Closed**: This state denotes the closing of a roll call and contains all the
**attendee** public keys scanned during the roll call process.  

## Creating a Roll-Call (roll_call#create)

🧭 **RPC Message** > **RPC payload** (*Query*) > **Query payload** (*Publish*) >
**Mid Level** > **High level** (*roll_call#create*)

An organizer creates a roll-call by publishing a roll_call/create message to the
LAO’s channel. The proposed_start and proposed_end fields denote the start and
end time for the roll call. Note that these fields are only used to position the
event correctly in the list of LAO events. Witnesses witness the roll call event
ensuring that each attendee is scanned exactly once.

<details>
<summary>
💡 See an example
</summary>

```json5
// ../protocol/examples/messageData/roll_call_create.json

{
    "object": "roll_call",
    "action": "create",
    "id": "XXX",
    "name": "XXX",
    "creation": 123,
    "proposed_start": 123,
    "proposed_end": 123,
    "location": "XXX",
    "description": "XXX"
}

```

</details>

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

## Opening a Roll-Call (roll_call#open)

A roll-call may be opened by the organizer by publishing a roll_call/open
message on the LAO channel.

<details>
<summary>
💡 See an example
</summary>

```json5
// ../protocol/examples/messageData/roll_call_open.json

{
    "object": "roll_call",
    "action": "open",
    "update_id": "XXX",
    "opens": "XXX",
    "opened_at": 123
}

```

</details>

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
            "enum": ["open", "reopen"]
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
    "required": ["object", "action", "update_id", "opens", "opened_at"],
    "note": [
        "The roll_call/open message opens a roll call. This message would lead to a transition from the",
        "created -> opened state after witnesses attest the message. Witnesses should attest this when they",
        "can ensure the roll call has actually started. roll_call/reopen may be used to re-open a",
        "closed roll call in case of a human error (forgot to scan a QR Code). A roll_call/reopen attested by",
        "witnesses would cause a transition from closed -> opened state"
    ]
}

```

## Closing a Roll-Call (roll_call#close)

A roll-call may be closed by the organizer by publishing a roll_call/close
message on the LAO channel. This is effectively the message that will be sent by
the organizer after scanning all attendees’ public key. 

![](assets/close_roll_call.png)

<details>
<summary>
💡 See an example
</summary>

```json5
// ../protocol/examples/messageData/roll_call_close.json

{
    "object": "roll_call",
    "action": "close",
    "update_id": "XXX",
    "closes": "XXX",
    "closed_at": 123,
    "attendees": ["XXX"]
}

```

</details>

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
            "$comment": "The 'update_id' of the latest roll call open/reopen"
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

## Reopening a Roll-Call (roll_call#reopen)

A closed roll-call may be re-opened by the organizer by publishing a
roll_call/reopen message on the LAO channel. This is useful in scenarios where
the organizer forgets to scan an attendee’s public key.

![](assets/reopen_roll_call.png)

<details>
<summary>
💡 See an example
</summary>

```json5
// ../protocol/examples/messageData/roll_call_reopen.json

{
    "object": "roll_call",
    "action": "reopen",
    "update_id": "XXX",
    "opens": "XXX",
    "opened_at": 123
}

```

</details>

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
            "enum": ["open", "reopen"]
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
    "required": ["object", "action", "update_id", "opens", "opened_at"],
    "note": [
        "The roll_call/open message opens a roll call. This message would lead to a transition from the",
        "created -> opened state after witnesses attest the message. Witnesses should attest this when they",
        "can ensure the roll call has actually started. roll_call/reopen may be used to re-open a",
        "closed roll call in case of a human error (forgot to scan a QR Code). A roll_call/reopen attested by",
        "witnesses would cause a transition from closed -> opened state"
    ]
}

```

# Social Media

## Posting a post by a user (post#user_post)

A post may be posted by a user on their own channel, only with an active PoP token. It consists of text (max. 280 Unicode code points), a Parent Id (if it is not the top level post) and of a timestamp (an UNIX stamp in UTC of the time the post is published).

<details>
<summary>
💡 See an example
</summary>

```json5
// ../protocol/examples/messageData/post_user_post.json

{
  "object": "post",
  "action": "add",
  "text": "My new tweet!", /* UTF-8 encoded post */
  "parent_id": "message_id", /* Either message_id of parent post, optional */
  "timestamp": 1631280815 /* UNIX Timestamp in UTC */
}

```

</details>

## Posting a post by a server (post#server_post)

After validating the post, the organizer's server propagates the message on the channel it is meant for, but also will create and send the following message to the universal post channel.

<details>
<summary>
💡 See an example
</summary>

```json5
// ../protocol/examples/messageData/post_server_post.json

{
  "object": "post",
  "action": "add",
  "post_id": "message_id", /* message_id of the post message above */
  "channel": "<channel>", /* The channel where the post is located (absolute path) */
  "timestamp": 1631280815 /* UNIX Timestamp in UTC given by the post message above */
}

```

</details>

## Removing a post by a user (post#user_remove)

A user may also remove their own post from their channel, only with an active PoP token.

<details>
<summary>
💡 See an example
</summary>

```json5
// ../protocol/examples/messageData/post_user_remove.json

{
  "object": "post",
  "action": "delete",
  "post_id": "message_id", /* Message id of the post published */
  "timestamp": 1631280815 /* UNIX Timestamp in UTC of this deletion request */
}

```

</details>

## Removing a post by a server (post#server_remove)

After a user has sent the message to remove their post, the server will propagate it to the channel it is meant for, but also will create and send the following message to the universal post channel.

<details>
<summary>
💡 See an example
</summary>

```json5
// ../protocol/examples/messageData/post_server_remove.json

{
  "object": "post",
  "action": "delete",
  "post_id": "message_id", /* message_id of the post message above */
  "channel": "<channel>", /* The channel where the post is located (starting from social/ inclusive) */
  "timestamp": 1631280815 /* UNIX Timestamp in UTC given by the message above */
}

```

</details>
