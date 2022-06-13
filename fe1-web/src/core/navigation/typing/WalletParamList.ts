import STRINGS from 'resources/strings';

export type WalletParamList = {
  [STRINGS.navigation_wallet_home]: undefined;

  [STRINGS.navigation_wallet_single_roll_call]: {
    rollCallId: string;
    rollCallName: string;
    rollCallTokenPublicKey: string;
  };

  [STRINGS.navigation_wallet_digital_cash_wallet]: {
    laoId: string;
  };

  [STRINGS.navigation_wallet_digital_cash_send_receive]: {
    laoId: string;
    /* undefined indicates coinbase transactions (coin issuance from the organizer) */
    rollCallId?: string;
    isCoinbase: boolean;

    /* parameters used to return from the scanner */
    scannedPoPToken?: string;
  };

  [STRINGS.navigation_wallet_digital_cash_wallet_scanner]: {
    laoId: string;
    rollCallId?: string;
    isCoinbase: boolean;
  };
};
