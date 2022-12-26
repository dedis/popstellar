import STRINGS from 'resources/strings';

export type WalletParamList = {
  [STRINGS.navigation_wallet_home]: undefined;

  [STRINGS.navigation_wallet_single_roll_call]: {
    rollCallId: string;
    rollCallName: string;
    rollCallTokenPublicKey: string;
  };
};
