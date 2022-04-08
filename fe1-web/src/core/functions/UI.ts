import { Timestamp } from 'core/objects';

const FIVE_MINUTES_IN_SECONDS = 300;

/**
 * Function called when the user confirms an event creation. If the end is in the past, it will tell
 * the user and cancel the creation. If the event starts more than 5 minutes in the past, it will
 * ask if it can start now. Otherwise, the event will simply be created.
 *
 * @param start - The start time of the event
 * @param end - The end time of the event
 * @param createEvent - The function which creates the event
 * @param setStartModalIsVisible - The function which sets the visibility of the modal on starting
 * time being in past
 * @param setEndModalIsVisible - The function which sets the visibility of the modal on ending time
 * being in past
 */
export const onConfirmEventCreation = (
  start: Timestamp,
  end: Timestamp,
  createEvent: Function,
  setStartModalIsVisible: Function,
  setEndModalIsVisible: Function,
) => {
  const now = Timestamp.EpochNow();

  if (end.before(now)) {
    setEndModalIsVisible(true);
  } else if (now.after(start.addSeconds(FIVE_MINUTES_IN_SECONDS))) {
    setStartModalIsVisible(true);
  } else {
    createEvent();
  }
};
