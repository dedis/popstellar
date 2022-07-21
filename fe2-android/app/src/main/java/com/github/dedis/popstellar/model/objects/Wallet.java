package com.github.dedis.popstellar.model.objects;

import android.util.Log;

import androidx.annotation.NonNull;

import com.github.dedis.popstellar.di.KeysetModule.WalletKeyset;
import com.github.dedis.popstellar.model.objects.security.PoPToken;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.ui.wallet.stellar.SLIP10;
import com.github.dedis.popstellar.utility.error.keys.*;
import com.google.crypto.tink.Aead;
import com.google.crypto.tink.integration.android.AndroidKeysetManager;

import net.i2p.crypto.eddsa.Utils;

import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.*;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.github.novacrypto.bip39.*;
import io.github.novacrypto.bip39.Validation.*;
import io.github.novacrypto.bip39.wordlists.English;

/**
 * This class represent a wallet that will enable users to store their PoP tokens with reasonable,
 * realistic security and usability.
 */
@Singleton
public class Wallet {

  private static final String TAG = Wallet.class.getSimpleName();
  private static final String PURPOSE = "888";
  private static final String ACCOUNT = "0";
  private byte[] seed;
  private Aead aead;

  /** Class constructor, initialize the wallet keyset. */
  @Inject
  public Wallet(@WalletKeyset AndroidKeysetManager keysetManager) {
    try {
      aead = keysetManager.getKeysetHandle().getPrimitive(Aead.class);
    } catch (GeneralSecurityException e) {
      Log.e(TAG, "Failed to initialize the Wallet", e);
      throw new IllegalStateException("Failed to initialize the Wallet", e);
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
  public PoPToken generatePoPToken(@NonNull String laoID, @NonNull String rollCallID)
      throws KeyGenerationException, UninitializedWalletException {
    // Generate the string path
    String res =
        String.join(
            "/", // delimiter
            "m",
            PURPOSE,
            ACCOUNT,
            convertDataToPath(laoID),
            convertDataToPath(rollCallID));

    Log.d(TAG, "Generated path: " + res);

    return generateKeyFromPath(res);
  }

  /**
   * Method that allows recovering of PoP Token, if the user has participated in that roll-call
   * event.
   *
   * @param laoID a String.
   * @param rollCallID a String.
   * @param rollCallTokens a {@link Set} containing the public keys of all attendees present on
   *     roll-call’s results.
   * @return the PoP Token if the user participated in that roll-call.
   * @throws KeyGenerationException if an error occurs during key generation
   * @throws UninitializedWalletException if the wallet is not initialized with a seed
   * @throws InvalidPoPTokenException if the token is not a valid attendee
   */
  public PoPToken recoverKey(
      @NonNull String laoID, @NonNull String rollCallID, @NonNull Set<PublicKey> rollCallTokens)
      throws KeyGenerationException, UninitializedWalletException, InvalidPoPTokenException {
    PoPToken token = generatePoPToken(laoID, rollCallID);
    if (rollCallTokens.contains(token.getPublicKey())) return token;
    else throw new InvalidPoPTokenException(token);
  }

  /**
   * Method that encode the seed into a form that is easier for humans to securely back-up and
   * retrieve.
   *
   * @return an array of words: mnemonic sentence representing the seed for the wallet in case that
   *     the key set manager is not init return a empty array.
   * @throws GeneralSecurityException if an error occurs
   */
  public String[] exportSeed() throws GeneralSecurityException {
    SecureRandom random = new SecureRandom();
    byte[] entropy = random.generateSeed(Words.TWELVE.byteLength());

    List<CharSequence> words = new LinkedList<>();
    MnemonicGenerator generator = new MnemonicGenerator(English.INSTANCE);
    generator.createMnemonic(entropy, words::add);

    String[] wordsFiltered =
        words.stream()
            .filter(s -> !s.equals(" ")) // Filter out spaces
            .map(CharSequence::toString)
            .toArray(String[]::new);

    Log.d(TAG, "the array of word generated:" + Arrays.toString(wordsFiltered));

    seed =
        aead.encrypt(new SeedCalculator().calculateSeed(String.join("", words), ""), new byte[0]);
    Log.d(TAG, "ExportSeed: new seed initialized: " + Utils.bytesToHex(seed));

    return wordsFiltered;
  }

  /**
   * Method that allow import mnemonic seed.
   *
   * @param words a String.
   */
  public void importSeed(@NonNull String words)
      throws SeedValidationException, GeneralSecurityException {

    try {
      MnemonicValidator.ofWordList(English.INSTANCE).validate(words);
    } catch (InvalidChecksumException
        | InvalidWordCountException
        | WordNotFoundException
        | UnexpectedWhiteSpaceException e) {
      throw new SeedValidationException(e);
    }

    seed = aead.encrypt(new SeedCalculator().calculateSeed(words, ""), new byte[0]);
    Log.d(TAG, "ImportSeed: new seed: " + Utils.bytesToHex(seed));
  }

  /**
   * Determine whether wallet has been initialized
   *
   * @return true if wallet has been set up, false otherwise
   */
  public boolean isSetUp() {
    return seed != null;
  }

  /** Logout the wallet by replacing the seed by a random one */
  public void logout() {
    seed = null;
  }

  /** Initialized the wallet with a new random seed */
  public void newSeed() {
    SecureRandom random = new SecureRandom();
    seed = random.generateSeed(64);
    Log.d(TAG, "Wallet initialized with a new random seed: " + Utils.bytesToHex(seed));
  }

  /**
   * This method allow to take a 256-bit string, and split it in many 24-bit or less string.
   *
   * <p>So, we first convert the string into an byte array, and we iterate on it taking 3 element
   * (byte) each time concatenate them and append to our result string.
   *
   * @param data to covert into a path
   * @return string of the form 3-byte/3-byte/...
   */
  private String convertDataToPath(String data) {
    // extract byte form string
    byte[] byteString = Base64.getUrlDecoder().decode(data);

    StringJoiner joiner = new StringJoiner("/");
    StringBuilder curPath = new StringBuilder();

    // create 31-bit index path
    for (int i = 0; i < byteString.length; i++) {
      curPath.append(byteString[i] & 0xFF);

      // Every 3 bytes, add the current path to the joiner and reset the builder
      if (i % 3 == 2) {
        joiner.add(curPath.toString());
        curPath.setLength(0);
      }
    }

    // If the path is not complete, add the remaining bytes to the joiner
    if (curPath.length() > 0) joiner.add(curPath.toString());

    return joiner.toString();
  }

  /**
   * Generate a PoPToken (i.e. a key pair) from a given path.
   *
   * @param path a String path of the form: m/i/j/k/... where i,j,k,.. are 31-bit integer.
   * @return the generated PoP Token
   * @throws KeyGenerationException if an error occurs
   * @throws UninitializedWalletException if the wallet is not initialized with a seed
   */
  private PoPToken generateKeyFromPath(@NonNull String path)
      throws KeyGenerationException, UninitializedWalletException {
    if (!isSetUp()) {
      throw new UninitializedWalletException();
    }

    // convert the path string in an array of int
    int[] pathValueInt =
        Arrays.stream(path.split("/"))
            .skip(1) // remove the first element ('m')
            .mapToInt(Integer::parseInt)
            .toArray();

    try {
      // derive private and public key
      byte[] privateKey =
          SLIP10.deriveEd25519PrivateKey(aead.decrypt(seed, new byte[0]), pathValueInt);

      Ed25519PrivateKeyParameters prK = new Ed25519PrivateKeyParameters(privateKey, 0);
      Ed25519PublicKeyParameters puK = prK.generatePublicKey();
      byte[] publicKey = puK.getEncoded();

      return new PoPToken(privateKey, publicKey);
    } catch (GeneralSecurityException e) {
      throw new KeyGenerationException(e);
    }
  }
}
