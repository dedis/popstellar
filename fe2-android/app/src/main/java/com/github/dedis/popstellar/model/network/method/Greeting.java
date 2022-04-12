package com.github.dedis.popstellar.model.network.method;

import com.github.dedis.popstellar.model.objects.Channel;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import java.util.Arrays;
import java.util.List;

/**
 * Greeting message, doesn't expect any answers, sent by the backend after connecting to it
 */
public class Greeting extends Message {

  // Backend sender address
  private PublicKey senderKey;
  // Backend server address
  private String address;
  // Backend "peer", list of addresses of future 1 Client -> multiple Server communication
  private List<String> peers;


  /**
   * Constructor for a Greeting Message
   * @param channel the channel over which the message is sent
   * @throws IllegalArgumentException if channel is null
   */
  public Greeting(Channel channel, String address, String senderKey, String... peers) {
    super(channel);
    if (address == null) {
      throw new IllegalArgumentException("The address of the backend can't be null");
    } else if (senderKey == null) {
      throw new IllegalArgumentException("The public key of the backend can't be null");
    }
    // Peers can be empty
    this.peers = Arrays.asList(peers);
    this.address = address;
    // Check of validity should be done via the Public Key class
    this.senderKey = new PublicKey(senderKey);
  }

  //Getter for params
  @Override
  public String getMethod() {
    return Method.GREETING.getMethod();
  }

  public PublicKey getSenderKey(){return senderKey;}

  public String getAddress(){return address;}

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
    boolean checkPeers =
        that.getPeers().containsAll(getPeers());

    return checkPeers && checkAddress;
  }

  @Override
  public String toString() {
    return "Greeting{" + "channel='" + getChannel().toString() + "', method='" + getMethod() + "', "
        + "sender = " + getSenderKey().toString() + "address=" + getAddress() + "}";
  }

}




