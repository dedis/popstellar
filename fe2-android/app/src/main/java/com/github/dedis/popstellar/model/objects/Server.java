package com.github.dedis.popstellar.model.objects;

import androidx.annotation.NonNull;

/**
 * A basic instance of the server what will be created while handling the
 * Greeting election message
 */
public class Server {

  // Canonical address of the server
  @NonNull private String serverAddress;

  // The public key of the server that can be used to send encrypted messages
  @NonNull private String publicKey;

  // NOTE: There is no need to store peers: ServerAddress[] here.
  // As soon as a greeting message arrives in the future, we connect to all peers. The server
  // addresses
  // will be added to the lao state as soon as a lao creation message is received
  // over each connection

  /**
   * Instantiate a server, for now it's a basic object which holds data about it, later, it will be
   * serialized and added to a list of peers server for 1 client --> multiple server
   *
   * @param serverAddress server's canonical address
   * @param publicKey server's public key
   */
  public Server(@NonNull String serverAddress, @NonNull String publicKey) {
    // Check validity of the params, how to check server's address?
    this.serverAddress = serverAddress;

    // Public key validity is checked while handling the greeting
    this.publicKey = publicKey;
  }

  /** Basic getters */
  @NonNull
  public String getPublicKey() {
    return publicKey;
  }

  @NonNull
  public String getServerAddress() {
    return serverAddress;
  }

}
