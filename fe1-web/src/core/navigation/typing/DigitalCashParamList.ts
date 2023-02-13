import STRINGS from 'resources/strings';

export type DigitalCashParamList = {
  [STRINGS.navigation_digital_cash_wallet]: {
    rollCallId?: string;
  };

  [STRINGS.navigation_digital_cash_send_receive]: {
    /* undefined indicates coinbase transactions (coin issuance from the organizer) */
    rollCallId?: string;
    isCoinbase: boolean;

    /* parameters used to return from the scanner */
    scannedPoPToken?: string;
  };

  [STRINGS.navigation_digital_cash_wallet_scanner]: {
    rollCallId?: string;
    isCoinbase: boolean;
  };
};
