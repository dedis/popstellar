import { useActionSheet as useExpoActionSheet } from '@expo/react-native-action-sheet';

import STRINGS from 'resources/strings';

export interface ActionSheetOption {
  displayName: string;
  action: () => void;
}

/**
 * Wrapper around 'useActionSheet' of expo/react-native-action-sheet
 * to provide a nicer interface
 */
export const useActionSheet = () => {
  const { showActionSheetWithOptions } = useExpoActionSheet();

  return (options: ActionSheetOption[]) =>
    showActionSheetWithOptions(
      {
        options: [...options.map((o) => o.displayName), STRINGS.general_button_cancel],
        cancelButtonIndex: options.length,
      },
      (idx) => {
        if (idx !== undefined && idx < options.length) {
          options[idx].action();
        }
      },
    );
};
