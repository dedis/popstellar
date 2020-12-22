import {
  shape, func, number, arrayOf, string, oneOf, object,
} from 'prop-types';

const PROPS_TYPE = {

  // --- LAO type ---
  LAO: shape({
    object: oneOf(['lao']).isRequired,
    action: oneOf(['state']).isRequired,
    id: string.isRequired,
    name: string.isRequired,
    creation: number.isRequired,
    last_modified: number.isRequired,
    organizer: string.isRequired,
    witnesses: arrayOf(string).isRequired,
    modification_id: string.isRequired,
    modification_signatures: arrayOf(shape({
      witness: string.isRequired,
      signature: string.isRequired,
    })).isRequired,
  }),

  // --- event type ---
  event: shape({
    object: oneOf(['lao', 'message', 'meeting', 'roll-call', 'poll', 'discussion']).isRequired,
    action: oneOf(['create', 'update_properties', 'state', 'witness', 'close', 'open', 'reopen']).isRequired,
    id: string.isRequired,
    name: string.isRequired,
    creation: number.isRequired,
    last_modified: number.isRequired,
    organizer: string.isRequired,
    witnesses: arrayOf(string).isRequired,
    children: arrayOf(this),
  }),

  // --- Meeting type ---
  meeting: shape({
    object: oneOf(['meeting']).isRequired,
    action: oneOf(['state']).isRequired,
    id: string.isRequired,
    name: string.isRequired,
    creation: number.isRequired,
    last_modified: number.isRequired,
    location: string,
    start: number.isRequired,
    end: number,
    // eslint-disable-next-line react/forbid-prop-types
    extra: object,
    organizer: string.isRequired,
    witnesses: arrayOf(string).isRequired,
    modification_id: string.isRequired,
    modification_signatures: arrayOf(shape({
      witness: string.isRequired,
      signature: string.isRequired,
    })).isRequired,
    // children: arrayOf(PROPS_TYPE.event), TODO find a way to implement this
  }),

  roll_call: shape({
    object: oneOf(['roll-call']).isRequired,
    action: oneOf(['close', 'open', 'reopen']).isRequired,
    name: string,
    id: string.isRequired,
    creation: number.isRequired,
    location: string,
    start: number,
    scheduled: number,
    end: number,
    attendees: arrayOf(string),
    roll_call_description: string,
    // children: arrayOf(PROPS_TYPE.event), TODO find a way to implement this
  }),

  // --- property type ---
  property: shape({
    object: string.isRequired,
    id: string.isRequired,
    name: string,
    witnesses: arrayOf(string),
  }),

  // --- navigation type of react-navigation (simplified) ---
  navigation: shape({
    navigate: func.isRequired,
    dangerouslyGetParent: func.isRequired,
    addListener: func.isRequired,
  }),

  // --- navigationState type of react-navigation (simplified) ---
  navigationState: shape({
    routes: arrayOf.isRequired,
  }),
};

export default PROPS_TYPE;
