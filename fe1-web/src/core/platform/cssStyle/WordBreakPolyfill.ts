import { Platform } from 'react-native';

/**
 * This polyfill is required to make sure that long words are broken for chirps
 * This is done by adding a css style to the head of the document
 */
export default () => {
  if (Platform.OS !== 'web' || typeof 'navigator' === undefined) {
    return;
  }

  const style = document.createElement('style');
  style.textContent = '* {word-break: break-word;}';
  document.head.append(style);
};
