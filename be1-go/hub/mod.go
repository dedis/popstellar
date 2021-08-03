// Package hub defines an interface that is used for processing incoming
// JSON-RPC messages from the websocket connection and replying to them
// by either sending a Result, Error or broadcasting a message to other
// clients.
//
// A concrete instance of a Hub may be an Organizer or a Witness. A baseHub
// type contains the implementation common across both types.
//
// The package also contains an implmentation of the Socket interface which
// is responsible for low level communication over websockets, i.e. sending
// and receiving marshaled messages over the wire.
//
// The Socket interface has multiple concrete implmentations - one for each
// type of client the backend interacts with: an Attendee, Organizer or Witness.
package hub
