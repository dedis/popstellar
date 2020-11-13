import PropTypes from 'prop-types';

const PROPS_TYPE = {
  LAO: PropTypes.shape({
    id: PropTypes.number.isRequired,
    name: PropTypes.string.isRequired,
    date: PropTypes.string.isRequired,
  }),

  event: PropTypes.shape({
    id: PropTypes.number.isRequired,
    type: PropTypes.string,
    name: PropTypes.string.isRequired,
    date: PropTypes.string.isRequired,
    children: PropTypes.arrayOf(this),
  }),

  navigation: PropTypes.shape({
    navigate: PropTypes.func.isRequired,
    dangerouslyGetParent: PropTypes.func.isRequired,
    addListener: PropTypes.func.isRequired,
  }),

  navigationState: PropTypes.shape({
    routes: PropTypes.arrayOf.isRequired,
  }),
};

export default PROPS_TYPE;
