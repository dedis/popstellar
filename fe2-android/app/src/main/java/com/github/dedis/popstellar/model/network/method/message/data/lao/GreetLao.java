package com.github.dedis.popstellar.model.network.method.message.data.lao;

import androidx.annotation.NonNull;

import com.github.dedis.popstellar.model.Immutable;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;
import com.github.dedis.popstellar.model.network.method.message.data.*;
import com.github.dedis.popstellar.model.objects.PeerAddress;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.google.gson.annotations.SerializedName;

import java.util.*;

import static com.github.dedis.popstellar.utility.DataCheckUtils.checkBase64;

@Immutable
public class GreetLao extends Data {

  @NonNull
  @SerializedName("lao")
  private final String id;

  // Backend sender address
  @NonNull
  @SerializedName("frontend")
  private final PublicKey frontendKey;

  // Backend server address
  @NonNull
  @SerializedName("address")
  private final String address;

  // Backend "peer", list of addresses of future (1 Client / multiple Servers) communication
  @SerializedName("peers")
  private final List<PeerAddress> peers;

  /**
   * Constructor for a data Greet LAO
   *
   * @param id id of the lao, should match Hash(organizer||creation||name) of the LAO creation
   *     message
   * @param frontend public key of the frontend of the server owner
   * @param address canonical address of the server with a protocol prefix and the port number
   * @param peers list of peers the server is connected to (excluding itself). These can be other
   *     organizers or witnesses
   * @throws IllegalArgumentException if arguments are invalid
   */
  public GreetLao(
      @NonNull String id,
      @NonNull String frontend,
      @NonNull String address,
      @NonNull List<PeerAddress> peers) {
    checkBase64(id, "lao_id");
    // Correctness of the id will be checked via the handler
    this.id = id;

    // Checking the validity of the public key is done via the Public Key class
    try {
      this.frontendKey = new PublicKey(frontend);
    } catch (Exception e) {
      throw new IllegalArgumentException("Please provide a valid public key");
    }

    // Validity of the address is checked at deserialization
    this.address = address;

    // Peers can be empty
    this.peers = new ArrayList<>(peers);
  }

  // Set of getters for t
  @NonNull
  public PublicKey getFrontendKey() {
    return frontendKey;
  }

  @NonNull
  public String getAddress() {
    return address;
  }

  public List<PeerAddress> getPeers() {
    return new ArrayList<>(peers);
  }

  @NonNull
  public String getId() {
    return id;
  }

  @Override
  public boolean equals(Object o) {

    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    GreetLao that = (GreetLao) o;

    boolean checkId = that.getId().equals(getId());
    boolean checkAddress = that.getAddress().equals(getAddress());
    boolean checkSendKey = that.getFrontendKey().equals(getFrontendKey());
    boolean checkPeers = that.getPeers().containsAll(getPeers());

    return checkId && checkPeers && checkSendKey && checkAddress;
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(frontendKey, address, peers);
  }

  @NonNull
  @Override
  public String toString() {
    return "GreetLao={"
        + "lao='"
        + getId()
        + '\''
        + ", frontend='"
        + getFrontendKey()
        + '\''
        + ", address='"
        + getAddress()
        + '\''
        + ", peers="
        + Arrays.toString(peers.toArray())
        + '}';
  }

  @Override
  public String getObject() {
    return Objects.LAO.getObject();
  }

  @Override
  public String getAction() {
    return Action.GREET.getAction();
  }
}
