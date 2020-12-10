const initialState = { organizationNavigation: false, LAO_ID: -1 };

/**
 * Reducer to switch between organization UI and home UI
 *
 * Pass the LAO id to the organization UI
 */

function toggleAppNavigationScreen(state = initialState, action) {
  let nextState;
  switch (action.type) {
    case 'APP_NAVIGATION_ON':
      nextState = {
        ...state,
        organizationNavigation: !state.organizationNavigation,
        LAO_ID: action.value,
      };
      return nextState || state;
    case 'APP_NAVIGATION_OFF':
      nextState = {
        ...state,
        organizationNavigation: !state.organizationNavigation,
        LAO_ID: -1,
      };
      return nextState || state;
    default:
      return state;
  }
}

export default toggleAppNavigationScreen;
