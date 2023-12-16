package com.github.dedis.popstellar.model.network.method.message.data.lao

import com.github.dedis.popstellar.model.Immutable
import com.github.dedis.popstellar.model.network.method.message.data.Action
import com.github.dedis.popstellar.model.network.method.message.data.Data
import com.github.dedis.popstellar.model.objects.PeerAddress
import com.github.dedis.popstellar.model.objects.security.PublicKey
import com.github.dedis.popstellar.utility.MessageValidator
import com.google.gson.annotations.SerializedName
import java.lang.IllegalArgumentException
import java.util.Arrays
import java.util.Objects

@Immutable
class GreetLao(
        id: String,
        frontend: String,
        address: String,
        peers: List<PeerAddress>) : Data() {
    @JvmField
    @SerializedName("lao")
    val id: String

    // Set of getters for t
    // Backend sender address
    @JvmField
    @SerializedName("frontend")
    var frontendKey: PublicKey? = null

    // Backend server address
    @JvmField
    @SerializedName("address")
    val address: String

    // Backend "peer", list of addresses of future (1 Client / multiple Servers) communication
    @SerializedName("peers")
    private val peers: List<PeerAddress>

    /**
     * Constructor for a Data GreetLao
     *
     * @param id id of the lao
     * @param frontend public key of the frontend of the server owner
     * @param address canonical address of the server with a protocol prefix and the port number
     * @param peers list of peers the server is connected to (excluding itself). These can be other
     * organizers or witnesses
     * @throws IllegalArgumentException if arguments are invalid
     */
    init {
        MessageValidator.verify().isNotEmptyBase64(id, "id")
        // Checking that the id matches the current lao id is done in the GreetLao handler
        this.id = id

        // Checking the validity of the public key is done via the Public Key class
        frontendKey = try {
            PublicKey(frontend)
        } catch (e: Exception) {
            throw IllegalArgumentException("Please provide a valid public key")
        }

        // Validity of the address is checked at deserialization
        this.address = address

        // Peers can be empty
        this.peers = ArrayList(peers)
    }

    fun getPeers(): List<PeerAddress> {
        return ArrayList(peers)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        val that = other as GreetLao
        val checkId = that.id == id
        val checkAddress = that.address == address
        val checkSendKey = that.frontendKey == frontendKey
        val checkPeers = HashSet(that.getPeers()).containsAll(getPeers())
        return checkId && checkPeers && checkSendKey && checkAddress
    }

    override fun hashCode(): Int {
        return Objects.hash(frontendKey, address, peers)
    }

    override fun toString(): String {
        return ("GreetLao={"
                + "lao='"
                + id
                + '\''
                + ", frontend='"
                + frontendKey
                + '\''
                + ", address='"
                + address
                + '\''
                + ", peers="
                + Arrays.toString(peers.toTypedArray())
                + '}')
    }

    override val `object`: String
        get() = com.github.dedis.popstellar.model.network.method.message.data.Objects.LAO.`object`
    override val action: String
        get() = Action.GREET.action
}