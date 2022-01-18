package com.github.dedis.popstellar.model.objects;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;

import com.github.dedis.popstellar.model.objects.security.PoPToken;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.ui.wallet.stellar.SLIP10;
import com.github.dedis.popstellar.utility.error.keys.KeyGenerationException;
import com.github.dedis.popstellar.utility.error.keys.SeedValidationException;
import com.github.dedis.popstellar.utility.error.keys.UninitializedWalletException;
import com.google.crypto.tink.Aead;
import com.google.crypto.tink.aead.AeadConfig;
import com.google.crypto.tink.aead.AesGcmKeyManager;
import com.google.crypto.tink.integration.android.AndroidKeysetManager;

import net.i2p.crypto.eddsa.Utils;

import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;
import io.github.novacrypto.bip39.MnemonicGenerator;
import io.github.novacrypto.bip39.MnemonicValidator;
import io.github.novacrypto.bip39.SeedCalculator;
import io.github.novacrypto.bip39.Validation.InvalidChecksumException;
import io.github.novacrypto.bip39.Validation.InvalidWordCountException;
import io.github.novacrypto.bip39.Validation.UnexpectedWhiteSpaceException;
import io.github.novacrypto.bip39.Validation.WordNotFoundException;
import io.github.novacrypto.bip39.Words;
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
  private boolean isSetup = false;

  /** Class constructor, initialize the wallet with a new random seed. */
  @Inject
  public Wallet(@ApplicationContext Context context) {
    setRandomSeed();
    try {
      initKeysManager(context);
    } catch (IOException | GeneralSecurityException e) {
      Log.e(TAG, "Failed to initialize the Wallet", e);
      throw new IllegalStateException("Failed to initialize the Wallet", e);
    }
  }

  /**
   * Method to overwrite the seed of the current wallet with a new seed.
   *
   * @param seed to apply
   */
  private void initialize(@NonNull byte[] seed) {
    this.seed = seed;
    isSetup = true;
    Log.d(TAG, "New seed initialized: " + Utils.bytesToHex(this.seed));
  }

  /**
   * Method to init the AndroidKeysetManager
   *
   * @param context of the application
   */
  private void initKeysManager(Context context) throws IOException, GeneralSecurityException {
    AesGcmKeyManager.register(true);
    AeadConfig.register();
    AndroidKeysetManager keysetManager =
        new AndroidKeysetManager.Builder()
            .withSharedPref(context, "POP_KEYSET_2", "POP_KEYSET_SP_2")
            .withKeyTemplate(AesGcmKeyManager.rawAes256GcmTemplate())
            .withMasterKeyUri("android-keystore://POP_MASTER_KEY_2")
            .build();

    aead = keysetManager.getKeysetHandle().getPrimitive(Aead.class);
  }

  /**
   * Generate a PoPToken (i.e. a key pair) from a given path.
   *
   * @param path a String path of the form: m/i/j/k/... where i,j,k,.. are 31-bit integer.
   * @return the generated PoP Token
   * @throws KeyGenerationException if an error occurs
   * @throws UninitializedWalletException if the wallet is not initialized with a seed
   */
  public PoPToken generateKeyFromPath(@NonNull String path)
      throws KeyGenerationException, UninitializedWalletException {
    if (!isSetup) {
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

  /**
   * Method that allows generate keys from the ID of the LAO and the ID of the RollCall.
   *
   * @param laoID a String.
   * @param rollCallID a String.
   * @return the PoP Token
   * @throws KeyGenerationException if an error occurs
   * @throws UninitializedWalletException if the wallet is not initialized with a seed
   */
  public PoPToken findKeyPair(@NonNull String laoID, @NonNull String rollCallID)
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
        curPath = new StringBuilder();
      }
    }

    // If the path is not complete, add the remaining bytes to the joiner
    if (curPath.length() > 0) joiner.add(curPath.toString());

    return joiner.toString();
  }

  /**
   * Method that allows recovering of PoP Token, if the user has participated in that roll-call
   * event.
   *
   * @param laoID a String.
   * @param rollCallID a String.
   * @param rollCallTokens a {@link Set} containing the public keys of all attendees present on
   *     roll-call’s results.
   * @return the PoP Token if the user participated in that roll-call or else empty.
   * @throws KeyGenerationException if an error occurs
   * @throws UninitializedWalletException if the wallet is not initialized with a seed
   */
  public Optional<PoPToken> recoverKey(
      @NonNull String laoID, @NonNull String rollCallID, @NonNull Set<PublicKey> rollCallTokens)
      throws KeyGenerationException, UninitializedWalletException {
    PoPToken token = findKeyPair(laoID, rollCallID);
    if (rollCallTokens.contains(token.getPublicKey())) return Optional.of(token);
    else return Optional.empty();
  }

  /**
   * Method that allows to recover all the pop tokens when the master secret is imported initially,
   * by iterating over all the historical LAO events.
   *
   * @param knowsLaosRollCalls a mapping of the pairs (Lao_ID, Roll_call_ID) to all the attendees
   *     public keys.
   * @return a mapping of the pairs (Lao_ID, Roll_call_ID) to the recovered tokens.
   * @throws KeyGenerationException if an error occurs
   * @throws UninitializedWalletException if the wallet is not initialized with a seed
   */
  private Map<Pair<String, String>, PoPToken> recoverAllKeys(
      @NonNull Map<Pair<String, String>, Set<PublicKey>> knowsLaosRollCalls)
      throws KeyGenerationException, UninitializedWalletException {

    Map<Pair<String, String>, PoPToken> result = new HashMap<>();
    for (Map.Entry<Pair<String, String>, Set<PublicKey>> entry : knowsLaosRollCalls.entrySet()) {
      String laoID = entry.getKey().first;
      String rollCallID = entry.getKey().second;

      Optional<PoPToken> recoverKey = recoverKey(laoID, rollCallID, entry.getValue());
      recoverKey.ifPresent(key -> result.put(new Pair<>(laoID, rollCallID), key));
    }

    return result;
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
    initialize(seed);
  }

  /**
   * Determine whether wallet has been initialized
   *
   * @return true if wallet has been set up, false otherwise
   */
  public boolean isSetUp() {
    return isSetup;
  }

  /** Logout the wallet by replacing the seed by a random one */
  public void logout() {
    setRandomSeed();
  }

  /** Utility function to initialize the wallet with a new random seed. */
  private void setRandomSeed() {
    SecureRandom random = new SecureRandom();
    seed = random.generateSeed(64);
    isSetup = false;
    Log.d(TAG, "Wallet initialized with a new random seed: " + Utils.bytesToHex(seed));
  }
}
