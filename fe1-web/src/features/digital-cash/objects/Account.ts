export type RollCallAccount = {
  rollCallId: string;
  rollCallName: string;
  popToken: string;

  /**
   * The balance of this account. null if it is the
   * organzer's coinbase account, i.e. if infinite money
   * is available
   */
  balance: null | number;
};
