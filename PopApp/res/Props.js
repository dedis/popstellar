import PropTypes from 'prop-types';

const PROPS_TYPE = {

  // --- LAO type ---
  LAO: PropTypes.shape({
    id: PropTypes.number.isRequired,
    name: PropTypes.string.isRequired,
    date: PropTypes.string.isRequired,
  }),

  // --- event type ---
  event: PropTypes.shape({
    id: PropTypes.number.isRequired,
    type: PropTypes.string.isRequired,
    name: PropTypes.string,
    date: PropTypes.string,
    children: PropTypes.arrayOf(this),
    witnesses: PropTypes.arrayOf(PropTypes.string),
  }),

  // --- navigation type of react-navigation (simplified) ---
  navigation: PropTypes.shape({
    navigate: PropTypes.func.isRequired,
    dangerouslyGetParent: PropTypes.func.isRequired,
    addListener: PropTypes.func.isRequired,
  }),

  // --- navigationState type of react-navigation (simplified) ---
  navigationState: PropTypes.shape({
    routes: PropTypes.arrayOf.isRequired,
  }),
};

export default PROPS_TYPE;
