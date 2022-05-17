/**
 * Creates a copy of an array and shuffles it
 * Copied from https://stackoverflow.com/a/46545530/2897827
 * WARNING: This function does not use a cryptographically safe source of randomness!
 * It simply uses Math.random(). For cryptographic purposes, reimplemented this
 * function using a better source of randomness
 * @param array The array to shuffle
 * @returns The shuffled array
 */
export const shuffleArray = <T extends unknown>(array: T[]): T[] =>
  array
    .map((value) => ({ value, sort: Math.random() }))
    .sort((a, b) => a.sort - b.sort)
    .map(({ value }) => value);
