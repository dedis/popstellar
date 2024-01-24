package com.github.dedis.popstellar.model.objects

import com.github.dedis.popstellar.model.Immutable
import com.github.dedis.popstellar.model.objects.security.PublicKey

/**
 * A basic instance of the server what will be created while handling the Greeting election message
 */
@Immutable
class Server // Check validity of the params, how to check server's address?

// Public key validity is checked while handling the greeting
// Public is cast as a String
/**
 * Instantiate a server, for now it's a basic object which holds data about it, later, it will be
 * serialized and added to a list of peers server for 1 client --> multiple server
 *
 * @param serverAddress server's canonical address
 * @param publicKey server's public key
 */
( // Canonical address of the server
    val serverAddress: String,
    /** Basic getters */
    // The public key of the server that can be used to send encrypted messages
    val publicKey: PublicKey
) {

  // NOTE: There is no need to store peers: ServerAddress[] here.
  // As soon as a GreetLao message arrives in the future, we connect to all peers. The server
  // addresses will first be added to the Server repository and then
  // will be added to the lao state as soon as a lao creation message is received
  // over each connection.
}
