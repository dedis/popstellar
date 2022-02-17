/**
 * @return the SubtleCrypto library on browser for browser context
 */
export function getSubtleCrypto() : SubtleCrypto {
  return window.crypto.subtle;
}
