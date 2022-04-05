package com.github.dedis.popstellar.model.network.method;

import com.github.dedis.popstellar.model.objects.Channel;
import java.util.Arrays;
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
  public Greeting(Channel channel) {
    super(channel);

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



}




