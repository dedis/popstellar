/**
 * Creates a copy of an array and shuffles it
 * Copied from https://stackoverflow.com/a/46545530/2897827
 * @param array The array to shuffle
 * @returns The shuffeled array
 */
export const shuffleArray = <T extends unknown>(array: T[]): T[] =>
  array
    .map((value) => ({ value, sort: Math.random() }))
    .sort((a, b) => a.sort - b.sort)
    .map(({ value }) => value);
