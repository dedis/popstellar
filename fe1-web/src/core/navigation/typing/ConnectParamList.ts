import STRINGS from 'resources/strings';

export type ConnectParamList = {
  [STRINGS.navigation_connect_scan]: undefined;
  [STRINGS.navigation_connect_confirm]:
    | {
        laoId: string;
        serverUrl: string;
      }
    | undefined;
  [STRINGS.navigation_connect_launch]: undefined;
};
