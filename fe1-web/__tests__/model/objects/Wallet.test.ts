import 'jest-extended';
import { HDWallet } from 'model/objects/HDWallet';
import { Hash } from 'model/objects';
import { WalletStore } from 'store/stores/WalletStore';

const mockMnemonicSeed: string = 'garbage effort river orphan negative kind outside quit hat camera approve first';

/**
 * This is the test class for the wallet. The test descriptions are self-explanatory.
 * The test cover both the functionality on the web front-end and cross-testing
 * compatibility with the Java front-end.
 */
describe('=== Hierarchical Deterministic Wallet Test ===', () => {
  describe('HD Wallet initialization test', () => {
    it('should initialize a wallet from mnemonic then destroy the wallet and retrieve it from redux state', async () => {
      let walletWhenInitialized: HDWallet | null = new HDWallet();
      let seedWhenInitialized: Uint8Array = new Uint8Array();

      walletWhenInitialized.initialize(mockMnemonicSeed)
        .then((isSuccessful) => expect(isSuccessful).toBe(true));

      walletWhenInitialized.getDecryptedSeed().then((plaintextSeed) => {
        seedWhenInitialized = plaintextSeed;
      });

      walletWhenInitialized = null;

      expect(walletWhenInitialized).toBeNull();

      let seedFromState: Uint8Array = new Uint8Array();

      WalletStore.get().then((encryptedSeed) => HDWallet
        .fromState(encryptedSeed).then((walletFromState) => {
          walletFromState.getDecryptedSeed().then((plaintextSeed) => {
            seedFromState = plaintextSeed;
          });
        }));

      expect(seedWhenInitialized).toStrictEqual(seedFromState);
    });
  });

  describe('HD Wallet seed cross-testing with Java front-end', () => {
    it('should correctly recover the SAME hex master key starting from the same mnemonic as the Java front-end', async () => {
      const wallet: HDWallet = new HDWallet();

      wallet.initialize(mockMnemonicSeed)
        .then((isSuccessful) => expect(isSuccessful)
          .toBe(true));

      const javaFrontEndPlaintextSeedFromMockMnemonic: string = '010ac98c615c31a20a6a9fcb71c94642abdd4f662d148f81d61479c8f125854bac9c0228f6705cbdd96e27ffb2d4e806d152c875a5484113434d1d561e42a94d';

      wallet.getDecryptedSeed().then((plaintextSeed) => {
        expect(Buffer.from(plaintextSeed).toString('hex')).toBe(javaFrontEndPlaintextSeedFromMockMnemonic);
      });
    });
  });

  describe('HD Wallet derivation cross-testing with Java front-end', () => {
    it('should correctly recover the SAME public key from path starting from the same wallet seed, LAO ID and roll call ID as the Java front-end', async () => {
      const wallet: HDWallet = new HDWallet();

      wallet.initialize(mockMnemonicSeed)
        .then((isSuccessful) => expect(isSuccessful).toBe(true));

      const laoId1: Hash = new Hash('T8grJq7LR9KGjE7741gXMqPny8xsLvsyBiwIFwoF7rg=');
      const laoId2: Hash = new Hash('SyJ3d9TdH8Ycb4hPSGQdArTRIdP9Moywi1Ux/Kzav4o=');

      const rollCallId1: Hash = new Hash('T8grJq7LR9KGjE7741gXMqPny8xsLvsyBiwIFwoF7rg=');
      const rollCallId2: Hash = new Hash('SyJ3d9TdH8Ycb4hPSGQdArTRIdP9Moywi1Ux/Kzav4o=');

      const testMap: Map<[Hash, Hash], string[]> = new Map();

      const javaFrontEndPopTokenForLao1RC1: string = '7147759d146897111bcf74f60a1948b1d3a22c9199a6b88c236eb7326adc2efc';
      const javaFrontEndPopTokenForLao2RC2: string = '2c23cfe90936a65839fb64dfb961690c3d8a5a1262f0156cf059b0c45a2eabff';

      testMap.set([laoId1, rollCallId1], [javaFrontEndPopTokenForLao1RC1, '']);
      testMap.set([laoId2, rollCallId2], ['', javaFrontEndPopTokenForLao2RC2]);

      let isFirstOfTwoTokens = true;
      wallet.recoverAllKeys(testMap).then((laoAndRcIdsToTokenMap) => {
        laoAndRcIdsToTokenMap.forEach((value, key) => {
          if (isFirstOfTwoTokens) {
            expect(key.toString()).toBe(`${laoId1},${rollCallId1}`);
            expect(value).toBe(javaFrontEndPopTokenForLao1RC1);
          } else {
            expect(key.toString()).toBe(`${laoId2},${rollCallId2}`);
            expect(value).toBe(javaFrontEndPopTokenForLao2RC2);
          }
          isFirstOfTwoTokens = false;
        });
      });
    });
  });
});
