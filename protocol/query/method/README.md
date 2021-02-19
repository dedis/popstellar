# student20_pop: proto-spec branch
Proof-of-personhood, fall 2020: Protocol specification

_There is a general README at the top-level of the branch._

# Low-level communication
The low-level communication takes care of establishing a channel-based publish/subscribe mechanism on the WebSocket connection. The implementation of the low-level communication layer is based on the JSON-RPC 2.0 protocol.

While WebSocket connections function as a bi-directional communication link, this protocol allows WebSocket clients to subscribe to existing pub/sub channels (and unsubscribe from them) as well as to publish a message on the pub/sub channel.
It is the responsibility of the organizer & witness servers to propagate any message received on pub/sub channel C across all open WebSocket connections listening to the pub/sub channel C.

To simplify the initialization of the system and keep the low-level communication simple, it is assumed that channel “/root” always exists, and only the server is allowed to subscribe to it. Clients can then publish on channel "/root" to create and bootstrap their Local Autonomous Organizer (LAO) (cf High-level communication).

All low-level, RPC-based communications carry an id field based on the JSON-RPC 2.0 specification. This field is a short-lived, unique id that is used to match the response with the corresponding request. It is assigned by whoever creates the request. How this field is generated is up to you, provided that there are never 2 concurrent requests with the same id in progress on the same WebSocket connection.
