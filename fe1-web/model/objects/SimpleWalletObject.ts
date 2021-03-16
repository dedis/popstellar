import { getWalletStore, KeyPairStore } from '../../store';

/**
 * @author Carlo Maria Musso
 *
 *
 */
export class SimpleWalletObject {
  private readonly walletId: string;

  /**
   * creates a wallet object
   */
  constructor() {
    this.walletId = KeyPairStore.getPublicKey().toString();
  }

  /**
   * this method creates the walletId entry in the secure wallet storage
   */
  public async initWalletStorage() {
    await getWalletStore()
      .createObjectStore(this.walletId);
  }

  /**
   * Adds the id of an attended roll call to a specific lao id entry.
   * The keypair in generated directly in the storage class
   * @param laoId the id of the LAO
   * @param rollCallId the id of the attended roll call
   */
  public async addTokenForRollCallAttendance(laoId: string, rollCallId: string) {
    if (laoId === null || rollCallId == null) {
      throw new Error('Error encountered while adding roll call to LAO : null argument');
    }

    getWalletStore()
      .putToken(this.walletId, laoId, rollCallId)
      .then((retVal) => console.log(`addTokenForRollCallAttendance return value is ${retVal}`));
  }

  /**
   * returns the keypair associated with the attendance of the roll
   * call organised by the LAO passed in parameter.
   * @param laoId the id of the LAO
   * @param rollCallId the id of the attended roll call
   */
  public async findKeyPair(laoId: string, rollCallId: string) {
    if (laoId === null || rollCallId == null) {
      throw new Error('Error encountered while finding Key Pair -> null argument');
    }
    getWalletStore()
      .getToken(this.walletId, laoId, rollCallId)
      .then((retVal) => console.log(`findKeyPair return value is ${retVal}`));
  }

  /**
   * deletes the keypair associated with the attendance of the roll
   * call organised by the LAO passed in parameter.
   * @param laoId the id of the LAO
   * @param rollCallId the id of the attended roll call
   */
  public async deleteRollCallToken(laoId: string, rollCallId: string) {
    if (laoId === null || rollCallId == null) {
      throw new Error('Error encountered while adding roll call to LAO : null argument');
    }
    getWalletStore()
      .deleteToken(this.walletId, laoId, rollCallId)
      .then((retVal) => console.log(`deleteRollCallToken return value is ${retVal}`));
  }
}
