package com.github.dedis.popstellar.model.network.method;

import com.github.dedis.popstellar.model.objects.Channel;
import java.util.List;

/**
 * Greeting message, doesn't expect any answers, sent by the backend after connecting to it
 */
public class Greeting extends Message {

  //Backend sender address
  private String sender;
  //Backend server address
  private String address;
  //Backend "peer", list of addresses of future 1 Client -> multiple Server communication
  private List<String> peers;


  /**
   * Constructor for a Greeting Message
   * @param channel the channel over which the message is sent
   * @throws IllegalArgumentException if channel is null
   */
  public Greeting(Channel channel, String address, String sender, List<String> peers) {
    super(channel);
    if (address == null) {
      throw new IllegalArgumentException("The address of the backend can't be null");
    } else if (sender == null) {
      throw new IllegalArgumentException("The public key of the backend can't be null");
    }
    //Peers can be empty
    this.peers = peers;
    this.address = address;
    this.sender = sender;
  }

  //Getter for params
  @Override
  public String getMethod() {
    return Method.GREETING.getMethod();
  }

  public String getSender(){return sender;}

  public String getAddress(){return address;}

  public List<String> getPeers(){
      //TODO: change the return format, might return null
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

    //Check fields
    boolean checkAdress = that.equals(getAddress());
    boolean checkSender = that.equals(getSender());
    boolean checkPeers = that.equals(getPeers());

    return checkSender && checkPeers && checkAdress;
  }

  @Override
  public String toString() {
    return "Greeting{" + "channel='" + getChannel() + "', method='" + getMethod() + "', "
        + "sender = " + getSender() + "address=" + getAddress() + "}";
  }


}




