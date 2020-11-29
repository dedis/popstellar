import { createStore } from 'redux';
import toggleAppNavigationScreen from './Reducers/appToggleReducer';

/**
 * Create the redux store for the app
 */

export default createStore(toggleAppNavigationScreen);
