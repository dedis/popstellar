package com.github.dedis.popstellar.model.objects;

import com.github.dedis.popstellar.model.objects.event.Event;
import com.github.dedis.popstellar.model.objects.event.EventState;
import com.github.dedis.popstellar.model.objects.event.EventType;
import com.github.dedis.popstellar.utility.security.Hash;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Consensus extends Event {

  private String channel;
  private String id;

  private String type;
  private String objId;
  private String property;
  private Object value;

  private long creation;
  private long end;

  private EventState state;
  private boolean isAccepted;

  private String proposer;
  private Set<String> acceptors;
  private Map<String, Boolean> acceptorsResponses;



  public Consensus(String type, String objId, long creation, String property, Object value) {
    this.id = generateConsensusId(creation, type, objId, property, value);
    this.type = type;
    this.objId = objId;
    this.property = property;
    this.value = value;

    this.isAccepted = false;
    this.acceptorsResponses = new HashMap<>();
  }


  public String getChannel() {
    return channel;
  }

  public void setChannel(String channel) {
    if (id == null) {
      throw new IllegalArgumentException("consensus channel shouldn't be null");
    }
    this.channel = channel;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    if (id == null) {
      throw new IllegalArgumentException("consensus id shouldn't be null");
    }
    this.id = id;
  }

  public String getConsensusType() {
    return type;
  }

  public void setType(String type) {
    if (type == null) {
      throw new IllegalArgumentException("consensus type shouldn't be null");
    }
    this.type = type;
  }

  public String getObjId() {
    return objId;
  }

  public void setObjId(String objId) {
    if (objId == null) {
      throw new IllegalArgumentException("consensus object id shouldn't be null");
    }
    this.objId = objId;
  }

  public String getProperty() {
    return property;
  }

  public void setProperty(String property) {
    if (property == null) {
      throw new IllegalArgumentException("consensus property shouldn't be null");
    }
    this.property = property;
  }

  public Object getValue() {
    return value;
  }

  public void setValue(Object value) {
    this.value = value;
  }

  public void setCreation(long creation) {
    if (creation < 0) {
      throw new IllegalArgumentException();
    }
    this.creation = creation;
  }

  public void setEnd(long end) {
    if (end < creation) {
      throw new IllegalArgumentException();
    }
    this.end = end;
  }


  public EventState getState() {
    return state;
  }

  public void setEventState(EventState state) {
    if (state == null) {
      throw new IllegalArgumentException("consensus state shouldn't be null");
    }
    this.state = state;
  }

  public String getProposer() {
    return proposer;
  }

  public void setProposer(String proposer) {
    if (proposer == null) {
      throw new IllegalArgumentException("consensus proposer shouldn't be null");
    }
    this.proposer = proposer;
  }

  public Set<String> getAcceptors() {
    return acceptors;
  }

  public void setAcceptors(Set<String> acceptors) {
    if (acceptors == null) {
      throw new IllegalArgumentException("consensus acceptors shouldn't be null");
    }
    this.acceptors = acceptors;
  }

  public Map<String, Boolean> getAcceptorsResponses() {
    return acceptorsResponses;
  }

  public void setAcceptorsResponses(Map<String, Boolean> acceptorsResponses) {
    this.acceptorsResponses = acceptorsResponses;
  }

  public void putAcceptorResponse(String acceptor, boolean accept) {
    if (acceptor == null) {
      throw new IllegalArgumentException("Acceptor public key cannot be null.");
    }
    acceptorsResponses.put(acceptor, accept);
  }

  public boolean isAccepted() {
    return isAccepted;
  }

  public void setAccepted(boolean accepted) {
    isAccepted = accepted;
  }


  @Override
  public long getStartTimestamp() {
    return creation;
  }

  @Override
  public EventType getType() {
    return EventType.CONSENSUS;
  }

  @Override
  public long getEndTimestamp() {
    return end;
  }



  public static String generateConsensusId(
      long createdAt, String type, String id, String property, Object value) {
    return Hash.hash(
        "consensus", Long.toString(createdAt), type, id, property, String.valueOf(value));
  }
}
