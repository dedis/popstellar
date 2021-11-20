export function requestCloseRollCall(
): Promise<void> {
  return new Promise((resolve) => {
    process.nextTick(() => resolve());
  });
}
