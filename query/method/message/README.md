# student20_pop: proto-spec branch
Proof-of-personhood, fall 2020: Protocol specification

_There is a general README at the top-level of the branch._

# Mid-level (message) communication
Building upon the low-level communication protocol, any communication is expressed as a message object being sent or received on a channel (cf. “Publishing a message on a channel”). This includes the creation of a LAO, changes to LAO properties, creation of meetings, roll-calls, polls, etc. 

As this layer builds on the low-level communication protocol, we already have operation confirmation / error management logic and don’t need to duplicate it.

## Message object
The message data specific to the operation is encapsulated in the data field, whereas the other fields attest to its :
Origin: sender field
Authenticity: signature and witness_signatures fields
Uniqueness: message_id field
```json
{
 "data": base64({}),  /* base64 representation of an object */
 "sender": "0x123a", /* Public key of sender */
 "signature": "0x123a", /* Signature by sender over "data" */
 "message_id": "0x123a", /* Hash : SHA256(Array of: Base64Data, Base64Signature) */
 "witness_signatures": [], /* Signature by witnesses (sender||data) */
}
```

The witness_signatures field will be empty when the message is first sent by the sender. It will subsequently be filled by the server, as it receives signatures by witnesses (asynchronously). When a client receives past messages, the witness signatures will be included. 

As you can see, the data field is base64 encoded. This derives from the necessity to hash and sign the field. An object cannot be signed, but one could imagine signing its JSON representation. However, a study of the JSON format will reveal that there's no "canonicalized", unique binary representation of JSON. That is, different systems may represent the same JSON object in different ways. The two following structures are identical JSON objects but have very different binary representations (field order, whitespace, newlines, unicode, etc.):

```json
{ "text": "message", "temp": "15\u00f8C" }
{"temp":"15°C","text":"message"}
```

As a consequence, we decide to encode the sender's preferred representation (any valid representation) in base64, and then use that representation as input to our hash or sign function. A nice side effect of this design is that the message object signature verification is entirely independent from the structure of the data field itself: no matter what message you send, the verification will always work in the same way (compare this to signing the data field-by-field!).
Once the data field is unencoded and parsed, the receiving peer can simply validate it and process it at the application level.

## Hashes
Hashes are made on arrays of base64 encoded strings which look like that ["Base64{Foo}", "Base64{Bar}"]. This is necessary to avoid some issues with inner "