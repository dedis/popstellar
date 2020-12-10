const initialState = { organizationNavigation: false, LAO_ID: -1 };

/**
 * Reducer to switch between organization UI and home UI
 *
 * Pass the LAO id to the organization UI
 *
 * Action types:
 *  - APP_NAVIGATION_ON: activate the organization UI and set LAO_ID to the value of the action
 *  - APP_NAVIGATION_OFF: activate the home UI and set LAO_ID to -1
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
