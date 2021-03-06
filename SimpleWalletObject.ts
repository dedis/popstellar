import { Hash } from "./Hash";
import { encodeBase64 } from "tweetnacl-util";
import { sign } from "tweetnacl";
import { KeyPair, KeyPairState } from "./KeyPair";

export class SimpleWalletObject {
  private walletId: Hash;
  private subscribedLaoIds: Set<Hash> = new Set<Hash>();
  private laoToRollCallsMap: Map<Hash, Set<Hash>> = new Map<Hash, Set<Hash>>();
  private rollCallsToKeyPairMap: Map<[Hash, Hash], KeyPairState> = new Map<
    [Hash, Hash],
    KeyPairState
  >();

  constructor(walletId: Hash) {
    if (walletId === null) {
      throw new Error(
        "Error encountered while creating a wallet object : undefined/null wallet Id"
      );
    }
    this.walletId = walletId;
  }

  // arrow function based on keyPair generation in "FromJsonData.test.ts"
  private generateKeyPair(): KeyPairState {
    const pair = sign.keyPair();
    const keys = {
      pubKey: encodeBase64(pair.publicKey),
      secKey: encodeBase64(pair.secretKey)
    };
    return {
      publicKey: keys.pubKey,
      privateKey: keys.secKey
    };
  }

  /**
   * Add a local autonomous organisation to the wallet
   * @param laoId the id for the LAO
   */
  public addLao(laoId: Hash) {
    if (laoId === null) {
      throw new Error("Error encountered while adding LAO -> null LAO ID");
    }
    this.subscribedLaoIds.add(laoId);
    this.laoToRollCallsMap.set(laoId, new Set<Hash>());
  }

  /**
   * Adds the id of an attended roll call to a specific lao id entry.
   * Also generates public and secret key pair to store in wallet via
   * a private method.
   * @param laoId the id of the LAO
   * @param rollCallId the id of the attended roll call
   */
  public addRollCallToLao(laoId: Hash, rollCallId: Hash) {
    if (laoId === null || rollCallId == null) {
      throw new Error(
        "Error encountered while adding roll call to LAO -> null argument"
      );
    }

    if (!this.subscribedLaoIds.has(laoId)) {
      this.addLao(laoId);
      this.laoToRollCallsMap.set(laoId, new Set<Hash>());
    }
    this.laoToRollCallsMap.set(
      laoId,
      this.laoToRollCallsMap.get(laoId).add(rollCallId)
    );

    this.generateKeyPairAndAddToWallet(laoId, rollCallId);

    console.log(
      "current database wallet size = " + this.rollCallsToKeyPairMap.size
    );
  }

  public findKeyPair = (laoId: Hash, rollCallId: Hash) => {
    if (laoId === null || rollCallId == null) {
      throw new Error(
        "Error encountered while finding Key Pair -> null argument"
      );
    }
    var key: [Hash, Hash] = [laoId, rollCallId];
    var pbK: string = ""; //this.rollCallsToKeyPairMap.get(key).publicKey;
    var prK: string = ""; //this.rollCallsToKeyPairMap.get(key).privateKey;
    // console.log("Private Key = " + pbK + "\nPublic Key = " + prK);
    return { publicKey: pbK, privateKey: prK };
  };

  /**
   * Generates the key pair and creates the mapping from laoId-rollCallId
   * to public/secret key pair
   * @param laoId the id of the LAO
   * @param rollCallId the id of the attended roll call
   */
  private generateKeyPairAndAddToWallet(laoId: Hash, rollCallId: Hash) {
    var laoAndRollKey: [Hash, Hash] = [laoId, rollCallId];
    var generatedKeyPair: KeyPairState = this.generateKeyPair();
    this.rollCallsToKeyPairMap.set(laoAndRollKey, generatedKeyPair);

    // DELETE THE BELOW CODE WHEN PROBLEM SOLVED
    var pbK: string = this.rollCallsToKeyPairMap.get(laoAndRollKey).publicKey;
    var prK: string = this.rollCallsToKeyPairMap.get(laoAndRollKey).privateKey;
    console.log("Added Private Key = " + pbK + "\n and Public Key = " + prK);
  }
}
