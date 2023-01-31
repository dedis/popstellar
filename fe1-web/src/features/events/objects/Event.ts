import PropTypes from 'prop-types';

import { ExtendType } from 'core/types';
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
export type EventState = ExtendType<
  PropTypes.InferType<typeof eventStatePropType>,
  { end?: number }
>;
