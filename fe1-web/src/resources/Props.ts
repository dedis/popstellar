import { Action } from '@react-navigation/routers/src/CommonActions';
import { arrayOf, number, object, oneOf, shape, string } from 'prop-types';

const PROPS_TYPE = {
  // --- LAO type ---
  LAO: shape({
    object: oneOf(['lao']).isRequired,
    action: oneOf(['state', 'create']).isRequired,
    id: string.isRequired,
    name: string.isRequired,
    creation: number.isRequired,
    last_modified: number,
    organizer: string.isRequired,
    witnesses: arrayOf(string).isRequired,
    modification_id: string,
    modification_signatures: arrayOf(
      shape({
        witness: string.isRequired,
        signature: string.isRequired,
      }),
    ),
  }),

  // --- event type ---
  event: shape({
    id: string.isRequired,
    start: number.isRequired,
    end: number,
  }),

  // --- Meeting type ---
  meeting: shape({
    id: string.isRequired,
    name: string.isRequired,
    creation: number.isRequired,
    last_modified: number.isRequired,
    start: number.isRequired,
    end: number,
    location: string,
    // eslint-disable-next-line react/forbid-prop-types
    extra: object,
  }),

  roll_call: shape({
    id: string.isRequired,
    name: string.isRequired,
    location: string.isRequired,
    description: string,
    creation: number.isRequired,
    proposed_start: number.isRequired,
    proposed_end: number.isRequired,
    opened_at: number,
    closed_at: number,
    status: number,
    attendees: arrayOf(string),
  }),

  // --- property type ---
  property: shape({
    object: string.isRequired,
    id: string.isRequired,
    name: string,
    witnesses: arrayOf(string),
  }),

  // --- notification type ---
  notification: shape({
    // WitnessFeature.MessageToWitnessNotification
    id: number.isRequired,
    timestamp: number.isRequired,
    title: string.isRequired,
    type: string.isRequired,
    messageId: string.isRequired,
  }),
};

export interface INavigation {
  navigate: (...args: any) => Action;
  addListener: (type: any, callback: any) => () => void;
}

export default PROPS_TYPE;
