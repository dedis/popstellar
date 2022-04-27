package com.github.dedis.popstellar.model.network.method.message.data.lao;

import androidx.annotation.NonNull;
import com.github.dedis.popstellar.model.network.method.Method;
import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Data;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;
import com.github.dedis.popstellar.model.objects.Channel;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.google.gson.annotations.SerializedName;
import java.util.Arrays;
import java.util.List;

public class GreetLao extends Data {

      @NonNull
      @SerializedName("lao")
      private final String id;

      // Backend sender address
      @NonNull
      @SerializedName("frontend")
      private String frontendKey;

      //Backend server address
      @NonNull
      @SerializedName("address")
      private String address;

      // Backend "peer", list of addresses of future 1 Client -> multiple Server communication
      @SerializedName("peers")
      private List<String> peers;

      /**
      * Constructor for a Greeting Message
      *
      * @throws IllegalArgumentException if channel is null
      */
      public GreetLao(@NonNull String id, @NonNull String frontend, @NonNull String address, String... peers) {

        // Peers can be empty
        this.peers = Arrays.asList(peers);
        this.address = address;

        // Check the validity of the public key should is done via the Public Key class
        try {
          new PublicKey(frontend);
        } catch (Exception e) {
          throw new IllegalArgumentException("Please provide a valid public key");
        }
        this.frontendKey = frontend;

        // Assume the id of the LAO will be checked via the handler
        this.id=id;
      }

      // Set of getters for t
      @NonNull
      public String getFrontendKey() {
        return frontendKey;
      }

      @NonNull
      public String getAddress() {
        return address;
      }

      public List<String> getPeers(){
      return peers;
    }

      @NonNull
      public String getId() {
    return id;
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

        GreetLao that = (GreetLao) o;

        // Check fields, how to equality check to keys ?
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

      @Override
      public String toString() {
        return "GreetLao={"
            + "lao='"
            + id
            + '\''
            + ", sender='"
            + getFrontendKey()
            + '\''
            + ", address='"
            + getAddress()
            + '\''
            + ", peers='"
            + Arrays.toString(peers.toArray())
            + "'}";
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

