/**
 * Shuffles an array in-place
 * Copied from https://stackoverflow.com/a/2450976/2897827
 * @param array The array to shuffle
 * @returns A reference to the shuffeled array
 */
export const shuffleArray = <T extends unknown>(array: T[]) => {
  let currentIndex = array.length;
  let randomIndex;

  // While there remain elements to shuffle...
  while (currentIndex !== 0) {
    // Pick a remaining element...
    randomIndex = Math.floor(Math.random() * currentIndex);
    currentIndex -= 1;

    // And swap it with the current element.

    // disable no-param-reassign eslint rule as we want the shuffle to be in-place
    // eslint-disable-next-line no-param-reassign
    [array[currentIndex], array[randomIndex]] = [array[randomIndex], array[currentIndex]];
  }

  return array;
};
