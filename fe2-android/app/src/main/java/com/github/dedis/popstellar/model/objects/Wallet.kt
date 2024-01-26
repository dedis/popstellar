package com.github.dedis.popstellar.model.objects

import com.github.dedis.popstellar.di.KeysetModule.WalletKeyset
import com.github.dedis.popstellar.model.objects.security.PoPToken
import com.github.dedis.popstellar.model.objects.security.PublicKey
import com.github.dedis.popstellar.ui.home.wallet.stellar.SLIP10
import com.github.dedis.popstellar.utility.error.keys.InvalidPoPTokenException
import com.github.dedis.popstellar.utility.error.keys.KeyGenerationException
import com.github.dedis.popstellar.utility.error.keys.SeedValidationException
import com.github.dedis.popstellar.utility.error.keys.UninitializedWalletException
import com.google.crypto.tink.Aead
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import io.github.novacrypto.bip39.MnemonicGenerator
import io.github.novacrypto.bip39.MnemonicValidator
import io.github.novacrypto.bip39.SeedCalculator
import io.github.novacrypto.bip39.Validation.InvalidChecksumException
import io.github.novacrypto.bip39.Validation.InvalidWordCountException
import io.github.novacrypto.bip39.Validation.UnexpectedWhiteSpaceException
import io.github.novacrypto.bip39.Validation.WordNotFoundException
import io.github.novacrypto.bip39.Words
import io.github.novacrypto.bip39.wordlists.English
import java.nio.charset.StandardCharsets
import java.security.GeneralSecurityException
import java.security.SecureRandom
import java.util.Arrays
import java.util.Base64
import java.util.StringJoiner
import javax.inject.Inject
import javax.inject.Singleton
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import timber.log.Timber

/**
 * This class represent a wallet that will enable users to store their PoP tokens with reasonable,
 * realistic security and usability.
 */
@Singleton
class Wallet @Inject constructor(@WalletKeyset keysetManager: AndroidKeysetManager) {
  private var encryptedSeed: ByteArray? = null
  private var encryptedMnemonic: ByteArray? = null

  private val aead: Aead

  /** Class constructor, initialize the wallet keyset. */
  init {
    aead =
        try {
          keysetManager.keysetHandle.getPrimitive(Aead::class.java)
        } catch (e: GeneralSecurityException) {
          Timber.tag(TAG).e(e, "Failed to initialize the Wallet")
          throw IllegalStateException("Failed to initialize the Wallet", e)
        }
  }

  /**
   * Generate a PoPToken from the ID of the LAO and the ID of the RollCall.
   *
   * @param laoID a String.
   * @param rollCallID a String.
   * @return the PoP Token
   * @throws KeyGenerationException if an error occurs
   * @throws UninitializedWalletException if the wallet is not initialized with a seed
   */
  @Throws(KeyGenerationException::class, UninitializedWalletException::class)
  fun generatePoPToken(laoID: String, rollCallID: String): PoPToken {
    // Generate the string path
    val res =
        java.lang.String.join(
            "/", // delimiter
            "m",
            PURPOSE,
            ACCOUNT,
            convertDataToPath(laoID),
            convertDataToPath(rollCallID))

    Timber.tag(TAG).d("Generated path: %s", res)

    return generateKeyFromPath(res)
  }

  /**
   * Method that allows recovering of PoP Token, if the user has participated in that roll-call
   * event.
   *
   * @param laoID a String.
   * @param rollCallID a String.
   * @param rollCallTokens a [Set] containing the public keys of all attendees present on
   *   roll-call’s results.
   * @return the PoP Token if the user participated in that roll-call.
   * @throws KeyGenerationException if an error occurs during key generation
   * @throws UninitializedWalletException if the wallet is not initialized with a seed
   * @throws InvalidPoPTokenException if the token is not a valid attendee
   */
  @Throws(
      KeyGenerationException::class,
      UninitializedWalletException::class,
      InvalidPoPTokenException::class)
  fun recoverKey(laoID: String, rollCallID: String, rollCallTokens: Set<PublicKey?>): PoPToken {
    val token = generatePoPToken(laoID, rollCallID)

    return if (rollCallTokens.contains(token.publicKey)) {
      token
    } else {
      throw InvalidPoPTokenException(token)
    }
  }

  /** @return the list of mnemonic words associated with the seed */
  @Throws(GeneralSecurityException::class)
  fun exportSeed(): Array<String> {
    if (encryptedMnemonic == null) {
      return emptyArray()
    }

    val decryptedBytes = aead.decrypt(encryptedMnemonic, ByteArray(0))
    val words = decryptedBytes.toString(StandardCharsets.UTF_8)

    Timber.tag(TAG).d("Mnemonic words successfully decrypted for export")

    return words.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
  }

  /**
   * Method that allow initialize wallet with mnemonic seed.
   *
   * @param words a String.
   */
  @Throws(SeedValidationException::class, GeneralSecurityException::class)
  fun importSeed(words: String) {
    try {
      MnemonicValidator.ofWordList(English.INSTANCE).validate(words)
    } catch (e: Exception) {
      when (e) {
        is InvalidChecksumException,
        is InvalidWordCountException,
        is WordNotFoundException,
        is UnexpectedWhiteSpaceException -> throw SeedValidationException(e)
        else -> throw e
      }
    }
    storeEncrypted(words)
    Timber.tag(TAG).d("Mnemonic words were successfully imported")
  }

  val isSetUp: Boolean
    /**
     * Determine whether wallet has been initialized
     *
     * @return true if wallet has been set up, false otherwise
     */
    get() = encryptedSeed != null

  /** Logout the wallet by replacing the seed by a random one */
  fun logout() {
    Timber.tag(TAG).d("Logged out of wallet")
    encryptedSeed = null
    encryptedMnemonic = null
  }

  /** Generates mnemonic seed but does not store it */
  fun newSeed(): String {
    val sb = StringBuilder()
    val entropy = ByteArray(Words.TWELVE.byteLength())
    SecureRandom().nextBytes(entropy)

    MnemonicGenerator(English.INSTANCE).createMnemonic(entropy) { s: CharSequence -> sb.append(s) }
    return sb.toString()
  }

  @Throws(GeneralSecurityException::class)
  private fun storeEncrypted(mnemonicWords: String) {
    encryptedMnemonic =
        aead.encrypt(mnemonicWords.toByteArray(StandardCharsets.UTF_8), ByteArray(0))

    encryptedSeed =
        aead.encrypt(
            SeedCalculator().calculateSeed(java.lang.String.join("", mnemonicWords), ""),
            ByteArray(0))

    Timber.tag(TAG).d("Mnemonic words and seed successfully encrypted")
  }

  /**
   * This method allow to take a 256-bit string, and split it in many 24-bit or less string.
   *
   * So, we first convert the string into an byte array, and we iterate on it taking 3 element
   * (byte) each time concatenate them and append to our result string.
   *
   * @param data to covert into a path
   * @return string of the form 3-byte/3-byte/...
   */
  private fun convertDataToPath(data: String): String {
    // extract byte form string
    val byteString = Base64.getUrlDecoder().decode(data)
    val joiner = StringJoiner("/")
    val curPath = StringBuilder()

    // create 31-bit index path
    for (i in byteString.indices) {
      curPath.append(byteString[i].toInt() and 0xFF)

      // Every 3 bytes, add the current path to the joiner and reset the builder
      if (i % 3 == 2) {
        joiner.add(curPath.toString())
        curPath.setLength(0)
      }
    }

    // If the path is not complete, add the remaining bytes to the joiner
    if (curPath.isNotEmpty()) {
      joiner.add(curPath.toString())
    }

    return joiner.toString()
  }

  /**
   * Generate a PoPToken (i.e. a key pair) from a given path.
   *
   * @param path a String path of the form: m/i/j/k/... where i,j,k,.. are 31-bit integer.
   * @return the generated PoP Token
   * @throws KeyGenerationException if an error occurs
   * @throws UninitializedWalletException if the wallet is not initialized with a seed
   */
  @Throws(KeyGenerationException::class, UninitializedWalletException::class)
  @Suppress("SpreadOperator")
  private fun generateKeyFromPath(path: String): PoPToken {
    if (!isSetUp) {
      throw UninitializedWalletException()
    }

    // convert the path string in an array of int
    val pathValueInt =
        Arrays.stream(path.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
            .skip(1) // remove the first element ('m')
            .mapToInt { s: String -> s.toInt() }
            .toArray()

    try {
      // derive private and public key
      val privateKey =
          SLIP10.deriveEd25519PrivateKey(aead.decrypt(encryptedSeed, ByteArray(0)), *pathValueInt)
      val prK = Ed25519PrivateKeyParameters(privateKey, 0)
      val puK = prK.generatePublicKey()
      val publicKey = puK.encoded

      return PoPToken(privateKey, publicKey)
    } catch (e: GeneralSecurityException) {
      throw KeyGenerationException(e)
    }
  }

  companion object {
    private val TAG = Wallet::class.java.simpleName
    private const val PURPOSE = "888"
    private const val ACCOUNT = "0"
  }
}
