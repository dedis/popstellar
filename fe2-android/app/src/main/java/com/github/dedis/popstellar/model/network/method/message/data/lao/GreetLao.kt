package com.github.dedis.popstellar.model.network.method.message.data.lao

import com.github.dedis.popstellar.model.Immutable
import com.github.dedis.popstellar.model.network.method.message.data.Action
import com.github.dedis.popstellar.model.network.method.message.data.Data
import com.github.dedis.popstellar.model.objects.PeerAddress
import com.github.dedis.popstellar.model.objects.security.PublicKey
import com.github.dedis.popstellar.utility.MessageValidator.verify
import com.google.gson.annotations.SerializedName
import java.lang.IllegalArgumentException
import java.util.Objects

@Immutable
class GreetLao(id: String, frontend: String, address: String, peers: List<PeerAddress>) : Data {
  @SerializedName("lao") val id: String

  // Backend sender address
  @SerializedName("frontend") var frontendKey: PublicKey

  // Backend server address
  @SerializedName("address") val address: String

  // Backend "peer", list of addresses of (1 Client / multiple Servers) communication
  @SerializedName("peers")
  val peers: List<PeerAddress>
    get() = ArrayList(field)

  /**
   * Constructor for a Data GreetLao
   *
   * @param id id of the lao
   * @param frontend public key of the frontend of the server owner
   * @param address canonical address of the server with a protocol prefix and the port number
   * @param peers list of peers the server is connected to (excluding itself). These can be other
   *   organizers or witnesses
   * @throws IllegalArgumentException if arguments are invalid
   */
  init {
    verify().isNotEmptyBase64(id, "id")

    // Checking that the id matches the current lao id is done in the GreetLao handler
    this.id = id

    // Checking the validity of the public key is done via the Public Key class
    this.frontendKey =
        try {
          PublicKey(frontend)
        } catch (e: Exception) {
          throw IllegalArgumentException("Please provide a valid public key\n$e")
        }

    // Validity of the address is checked at deserialization
    this.address = address

    // Peers can be empty
    this.peers = ArrayList(peers)
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other == null || javaClass != other.javaClass) {
      return false
    }
    val that = other as GreetLao
    return that.id == id &&
        that.address == address &&
        that.frontendKey == frontendKey &&
        that.peers == peers
  }

  override fun hashCode(): Int {
    return Objects.hash(frontendKey, address, peers)
  }

  override fun toString(): String {
    return "GreetLao={lao='$id', frontend='$frontendKey', address='$address', peers=${
      peers.toTypedArray().contentToString()
    }}"
  }

  override val `object`: String
    get() = com.github.dedis.popstellar.model.network.method.message.data.Objects.LAO.`object`

  override val action: String
    get() = Action.GREET.action
}
