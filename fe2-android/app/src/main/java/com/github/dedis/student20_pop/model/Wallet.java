package com.github.dedis.student20_pop.model;

import androidx.core.util.Pair;
import io.github.novacrypto.bip39.MnemonicGenerator;
import io.github.novacrypto.bip39.MnemonicValidator;
import io.github.novacrypto.bip39.SeedCalculator;
import io.github.novacrypto.bip39.Words;
import io.github.novacrypto.bip39.wordlists.English;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import javax.crypto.Mac;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.SecretKeySpec;
import net.i2p.crypto.eddsa.Utils;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;

/**
 * This class represent a wallet that will enable users to store their PoP tokens with reasonable,
 * realistic security and usability.
 */
public class Wallet {

  private static final String TAG = Wallet.class.getSimpleName();
  private static final String hmacSHA512algorithm = "HmacSHA512";
  private static final int PURPOSE =  888;
  private static final int ACCOUNT =  0;
  private byte[] SEED;

  /**
   * Class constructor, initialize the wallet with a new random seed.
   */
  public Wallet() {
    SecureRandom random = new SecureRandom();
    byte[] bytes = random.generateSeed(64); // max nb byte (512 bit): 256 bits is advised.
    SEED = bytes;
    System.out.println( "Wallet: initialized with a new random seed: " + Utils.bytesToHex(SEED));
  }

  /**
   * Method to overwrite the seed of the current wallet with a new seed.
   *
   * @param seed  A String representing the 256-bit seed.
   */
  public void initialize(String seed){
    if (seed == null) {
      throw new UnsupportedOperationException("Unable to init seed from a null param!");
    }
    SEED = Utils.hexToBytes(seed);
    System.out.println( "initialize: new seed initialized: " + Utils.bytesToHex(SEED));
  }

  /**
   * Method that allow generate a different key for each path that you give.
   *
   * @param path a String path of the form: m/i/j/k/... where i,j,k,.. are 31-bit integer.
   * @return a Pair<byte[], byte[]> representing the keys pair: first=private_key; second=public_key.
   * @throws NoSuchAlgorithmException
   * @throws InvalidKeyException
   * @throws ShortBufferException
   */
  public Pair<byte[], byte[]> GenerateKeyFromPath(String path)
      throws NoSuchAlgorithmException, InvalidKeyException, ShortBufferException {
    if (path == null) {
      throw new UnsupportedOperationException("Unable to find keys from a null path!");
    }
    //split the path string
    List<String> path_value = new ArrayList<>(Arrays.asList(path.split("/")));
    System.out.println( "Path decomposed: " + path_value);

    path_value.remove(0); //remove the first element (m)

    //convert the path string in an array of int
    int[] path_value_int = path_value
        .stream()
        .map(Integer::parseInt)
        .mapToInt(Integer::intValue).toArray();

    System.out.print("Integer path: {  ");
    for (int i: path_value_int) System.out.print(i+"  ");
    System.out.println("} ");

    // derive private and public key
    byte[] private_key = deriveEd25519PrivateKey(SEED, path_value_int);
    Ed25519PrivateKeyParameters pr_k = new Ed25519PrivateKeyParameters(private_key, 0);
    Ed25519PublicKeyParameters pu_k = pr_k.generatePublicKey();
    byte[] public_key = pu_k.getEncoded();

    return new Pair<>(private_key, public_key);
  }

  /**
   * Method that allows generate keys from the ID of the LAO and the ID of the RollCall.
   *
   * @param Lao_ID a String.
   * @param Roll_call_ID a String.
   * @return a Pair<byte[], byte[]> representing the keys pair.
   * @throws NoSuchAlgorithmException
   * @throws InvalidKeyException
   * @throws ShortBufferException
   */
  public Pair<byte[], byte[]> FindKeyPair(String Lao_ID, String Roll_call_ID)
      throws NoSuchAlgorithmException, InvalidKeyException, ShortBufferException {
    if (Lao_ID == null || Roll_call_ID == null) {
      throw new UnsupportedOperationException("Unable to find keys from a null param");
    }
    //Generate the string path
    StringJoiner joiner = new StringJoiner("/");
    joiner.add("m");
    joiner.add(Integer.toString(PURPOSE));
    joiner.add(Integer.toString(ACCOUNT));
    joiner.add(convert_string_to_path(Lao_ID));
    joiner.add(convert_string_to_path(Roll_call_ID));
    String res = joiner.toString();

    System.out.println("Generated path: " + res);

    return GenerateKeyFromPath(res);
  }

  private String convert_string_to_path(String string){
    // extract byte form string
    byte[] byte_string = Base64.getDecoder().decode(string);

    // create 31-bit index path
    StringJoiner joiner = new StringJoiner("/");
    int i = 0;
    while(i < byte_string.length){
      int res = 0;
      for(int j=i; j<i+3 && j< byte_string.length; j++){
        int buffer =  byte_string[j];
        buffer = buffer & 0xFF; // "extract" the first 8 bit
        res = (buffer << j%3*8) | res;
      }
      i+= 3;
      joiner.add(Integer.toString(res));
    }

    return joiner.toString();
  }

  /**
   * Method that allows recover key pair, if the user has participated in that roll-call event.
   *
   * @param Lao_ID a String.
   * @param Roll_call_ID a String.
   * @param Roll_call_Tokens a List<byte[]> representing the list of public keys present
   *                         on roll-call’s results.
   * @return the key pair Pair<byte[], byte[]> (PoP token) if the user as in that roll-call
   * participated else null.
   * @throws NoSuchAlgorithmException
   * @throws InvalidKeyException
   * @throws ShortBufferException
   */
  public Pair<byte[], byte[]> RecoverKey(String Lao_ID, String Roll_call_ID,
      List<byte[]> Roll_call_Tokens)
      throws NoSuchAlgorithmException, InvalidKeyException, ShortBufferException {

    Pair<byte[], byte[]> key_pair_find = FindKeyPair(Lao_ID,Roll_call_ID);
    for(byte[] public_key : Roll_call_Tokens){
      if(Arrays.equals(key_pair_find.second, public_key)){
        return key_pair_find;
      }
    }
    return null;
  }

  /**
   * Method that allows recover recover all the key pairs when the master secret is imported
   * initially, by iterating all the historical events of LAO.
   *
   * @param seed the master secret String
   * @param knows_Laos_Roll_calls a Map<Pair<String, String>, List<byte[]>> of keys known Lao_ID
   *                              and Roll_call_ID and values representing the list of public keys
   *                              present on roll-call’s results.
   * @return a Map<Pair<String, String>, Pair<byte[], byte[]>> of the recover key pairs
   * associated to each Lao and roll-call IDs.
   * @throws NoSuchAlgorithmException
   * @throws InvalidKeyException
   * @throws ShortBufferException
   */
  public Map<Pair<String, String>, Pair<byte[], byte[]>> RecoverAllKeys(String seed,
      Map<Pair<String, String>, List<byte[]>>  knows_Laos_Roll_calls)
      throws NoSuchAlgorithmException, InvalidKeyException, ShortBufferException {

    initialize(seed);

    Map<Pair<String, String>, Pair<byte[], byte[]>> result = new HashMap<>();
    for (Map.Entry<Pair<String, String>, List<byte[]>> entry : knows_Laos_Roll_calls.entrySet()) {
      String Lao_ID = entry.getKey().first;
      String Roll_call_ID = entry.getKey().second;
      Pair<byte[], byte[]> recovered_key = RecoverKey(Lao_ID,  Roll_call_ID, entry.getValue());
      if(recovered_key != null){
        result.put(new Pair<>(Lao_ID, Roll_call_ID), recovered_key);
      }
    }
    return result;
  }

  /**
   * Method that encode the seed into a form that is easier for humans to securely back-up
   * and retrieve.
   *
   * @return an array of words: mnemonic sentence representing the seed for the wallet.
   */
  public String[] ExportSeed(){

    SecureRandom random = new SecureRandom();
    byte[] entropy = random.generateSeed(Words.TWELVE.byteLength());

    StringBuilder sb = new StringBuilder();
    MnemonicGenerator generator = new MnemonicGenerator(English.INSTANCE);
    generator.createMnemonic(entropy, sb::append);

    String[] words = sb.toString().split(" ");
    System.out.println(Arrays.toString(words));

    StringJoiner joiner = new StringJoiner(" ");
    for(String i: words) joiner.add(i);
    SEED = new SeedCalculator().calculateSeed(joiner.toString(), "");
    System.out.println( "ExportSeed: new seed initialized: " + Utils.bytesToHex(SEED));

    return words;
  }

  /**
   * Method that allow import mnemonic seed.
   *
   * @param words a String.
   * @param knows_Laos_Roll_calls a Map<Pair<String, String>, List<byte[]>> of keys known Lao_ID
   *                              and Roll_call_ID and values representing the list of public keys
   *                              present on roll-call’s results.
   * @return a Map<Pair<String, String>, Pair<byte[], byte[]>> of the recover key pairs
   *         associated to each Lao and roll-call IDs or null in case of error.
   */
  /**
   * Method that allow import mnemonic seed.
   *
   * @param words a String.
   * @param knows_Laos_Roll_calls a Map<Pair<String, String>, List<byte[]>> of keys known Lao_ID
   *                              and Roll_call_ID and values representing the list of public keys
   *                              present on roll-call’s results.
   * @return a Map<Pair<String, String>, Pair<byte[], byte[]>> of the recover key pairs
   *         associated to each Lao and roll-call IDs or null in case of error.
   * @throws NoSuchAlgorithmException
   * @throws InvalidKeyException
   * @throws ShortBufferException
   */
  public Map<Pair<String, String>, Pair<byte[], byte[]>> ImportSeed(String words,
      Map<Pair<String, String>, List<byte[]>>  knows_Laos_Roll_calls)
      throws NoSuchAlgorithmException, InvalidKeyException, ShortBufferException {

    try {
      MnemonicValidator
          .ofWordList(English.INSTANCE)
          .validate(words);
      SEED = new SeedCalculator().calculateSeed(words, "");
      System.out.println( "ImportSeed: new seed: " + Utils.bytesToHex(SEED));

    } catch (Exception e) {
      System.out.println("Unable to import words:" + e.getMessage());
      return null;
    }
    return RecoverAllKeys(Utils.bytesToHex(SEED), knows_Laos_Roll_calls);
  }


  /**
   * Derives only the private key for ED25519 in the manor defined in
   * <a href="https://github.com/satoshilabs/slips/blob/master/slip-0010.md">SLIP-0010</a>.
   *
   * @param seed    Seed, the BIP0039 output.
   * @param indexes an array of indexes that define the path. E.g. for m/1'/2'/3', pass 1, 2, 3.
   *                As with Ed25519 non-hardened child indexes are not supported, this function
   *                treats all indexes as hardened.
   * @return Private key.
   * @throws NoSuchAlgorithmException If it cannot find the HmacSHA512 algorithm by name.
   * @throws ShortBufferException     Occurrence not expected.
   * @throws InvalidKeyException      Occurrence not expected.
   */
  private byte[] deriveEd25519PrivateKey(final byte[] seed, final int... indexes)
      throws NoSuchAlgorithmException, ShortBufferException, InvalidKeyException {

    final byte[] I = new byte[64];
    final Mac mac = Mac.getInstance(hmacSHA512algorithm);

    // I = HMAC-SHA512(Key = bytes("ed25519 seed"), Data = seed)
    mac.init(new SecretKeySpec("ed25519 seed".getBytes(Charset.forName("UTF-8")), hmacSHA512algorithm));
    mac.update(seed);
    mac.doFinal(I, 0);

    for (int i : indexes) {
      // I = HMAC-SHA512(Key = c_par, Data = 0x00 || ser256(k_par) || ser32(i'))
      // which is simply:
      // I = HMAC-SHA512(Key = Ir, Data = 0x00 || Il || ser32(i'))
      // Key = Ir
      mac.init(new SecretKeySpec(I, 32, 32, hmacSHA512algorithm));
      // Data = 0x00
      mac.update((byte) 0x00);
      // Data += Il
      mac.update(I, 0, 32);
      // Data += ser32(i')
      mac.update((byte) (i >> 24 | 0x80));
      mac.update((byte) (i >> 16));
      mac.update((byte) (i >> 8));
      mac.update((byte) i);
      // Write to I
      mac.doFinal(I, 0);
    }

    final byte[] Il = new byte[32];
    // copy head 32 bytes of I into Il
    System.arraycopy(I, 0, Il, 0, 32);
    return Il;
  }
}
