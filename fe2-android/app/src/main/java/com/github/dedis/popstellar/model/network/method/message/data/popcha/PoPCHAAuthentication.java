package com.github.dedis.popstellar.model.network.method.message.data.popcha;

import androidx.annotation.Nullable;
import com.github.dedis.popstellar.model.Immutable;
import com.github.dedis.popstellar.model.network.method.message.data.*;
import com.google.gson.annotations.SerializedName;

/** Data sent to authenticate to a PoPCHA server */
@Immutable
public class PoPCHAAuthentication extends Data {

  @SerializedName("client_id")
  private final String clientId;

  private final String nonce;
  private final String identifier;

  @SerializedName("identifier_proof")
  private final String identifierProof;

  @Nullable private final String state;

  @Nullable
  @SerializedName("response_mode")
  private final String responseMode;

  @SerializedName("popcha_address")
  private final String popchaAddress;

  public PoPCHAAuthentication(
      String clientId,
      String nonce,
      String identifier,
      String identifierProof,
      @Nullable String state,
      @Nullable String responseMode,
      String popchaAddress) {
    this.clientId = clientId;
    this.nonce = nonce;
    this.identifier = identifier;
    this.identifierProof = identifierProof;
    this.state = state;
    this.responseMode = responseMode;
    this.popchaAddress = popchaAddress;
  }

  @Override
  public String getObject() {
    return Objects.POPCHA.getObject();
  }

  @Override
  public String getAction() {
    return Action.AUTH.getAction();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    PoPCHAAuthentication that = (PoPCHAAuthentication) o;
    return clientId.equals(that.clientId)
        && nonce.equals(that.nonce)
        && identifier.equals(that.identifier)
        && identifierProof.equals(that.identifierProof)
        && java.util.Objects.equals(state, that.state)
        && java.util.Objects.equals(responseMode, that.responseMode)
        && popchaAddress.equals(that.popchaAddress);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(
        clientId, nonce, identifier, identifierProof, state, responseMode, popchaAddress);
  }

  @Override
  public String toString() {
    return "PoPCHAAuthentication{"
        + "clientId='"
        + clientId
        + '\''
        + ", nonce='"
        + nonce
        + '\''
        + ", identifier='"
        + identifier
        + '\''
        + ", identifierProof='"
        + identifierProof
        + '\''
        + ", state='"
        + state
        + '\''
        + ", responseMode='"
        + responseMode
        + '\''
        + ", popchaAddress='"
        + popchaAddress
        + '\''
        + '}';
  }
}
