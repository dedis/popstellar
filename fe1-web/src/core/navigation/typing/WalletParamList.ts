import STRINGS from 'resources/strings';

export type WalletParamList = {
  [STRINGS.navigation_wallet_home]: undefined;

  [STRINGS.navigation_wallet_single_roll_call]: {
    rollCallId: string;
    rollCallName: string;
    rollCallTokenPublicKey: string;
  };

  [STRINGS.navigation_wallet_digital_cash_wallet]: undefined;

  [STRINGS.navigation_wallet_digital_cash_send_receive]: {
    /* undefined indicates coinbase transactions (coin issuance from the organizer) */
    rollCallId?: string;
    isCoinbase: boolean;

    /* parameters used to return from the scanner */
    scannedPoPToken?: string;
  };

  [STRINGS.navigation_wallet_digital_cash_wallet_scanner]: {
    rollCallId?: string;
    isCoinbase: boolean;
  };
};
