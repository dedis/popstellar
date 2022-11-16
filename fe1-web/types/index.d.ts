/* eslint-disable no-var */
/* eslint-disable vars-on-top */

type ToastType = import('react-native-toast-notifications').ToastType;

declare global {
  var toast: ToastType | undefined | null;
}

declare let toast: ToastType;

export {};
