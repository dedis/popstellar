import PropTypes from 'prop-types';
/**
 * Interface to represent an event within a LAO.
 */

export const eventStatePropType = PropTypes.shape({
  eventType: PropTypes.string.isRequired,
  id: PropTypes.string.isRequired,

  start: PropTypes.number.isRequired,
  end: PropTypes.number,
}).isRequired;

// Serializable Event (using primitive types)
export type EventState = PropTypes.InferType<typeof eventStatePropType>;
