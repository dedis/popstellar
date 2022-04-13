package com.github.dedis.popstellar.model.network.method;

import androidx.annotation.NonNull;
import com.github.dedis.popstellar.model.objects.Channel;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.google.gson.annotations.SerializedName;
import java.util.Arrays;
import java.util.List;

/**
 * Greeting message, doesn't expect any answers, sent by the backend after connecting to it
 */
public class Greeting extends Message {

  // Backend sender address
  @NonNull
  @SerializedName("sender")
  private String senderKey;

  // Backend server address
  @NonNull
  @SerializedName("address")
  private String address;

  // Backend "peer", list of addresses of future 1 Client -> multiple Server communication
  @SerializedName("peers")
  private List<String> peers;

  /**
   * Constructor for a Greeting Message
   *
   * @param channel the channel over which the message is sent
   * @throws IllegalArgumentException if channel is null
   */
  public Greeting(
      Channel channel, @NonNull String senderKey, @NonNull String address, String... peers) {
    super(channel);
    // Peers can be empty
    this.peers = Arrays.asList(peers);
    this.address = address;
    // Check of validity of the public key should be done via the Public Key class
    try {
      new PublicKey(senderKey);
    } catch (Exception e) {
      throw new IllegalArgumentException("Please provide a valid public key");
    }
    this.senderKey = senderKey;
  }

  //Getter for params
  @Override
  public String getMethod() {
    return Method.GREETING.getMethod();
  }

  @NonNull
  public String getSenderKey() {
    return senderKey;}

  @NonNull
  public String getAddress() {
    return address;}

  public List<String> getPeers(){
      return peers;
  }

  @Override
  public boolean equals(Object o){
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }

    Greeting that = (Greeting) o;

    // Check fields, how to equality check to keys ?
    boolean checkAddress = that.getAddress().equals(getAddress());
    boolean checkSendKey = that.getSenderKey().equals(getSenderKey());
    boolean checkPeers =
        that.getPeers().containsAll(getPeers());

    return checkPeers && checkAddress;
  }

  @Override
  public String toString() {
    return "Greeting={"
        + "channel='"
        + getChannel().toString()
        + '\''
        + ", sender='"
        + getSenderKey()
        + '\''
        + ", address='"
        + getAddress()
        + '\''
        + ", peers='"
        + Arrays.toString(peers.toArray())
        + "'}";
  }

}




