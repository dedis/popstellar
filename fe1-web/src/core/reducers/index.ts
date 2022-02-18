/**
 * The reducers module contains the Redux reducers used by the project,
 * as well as the selectors.
 *
 * A reducer (within the Redux framework) is nothing more than a function
 * taking the current state, an action, and returning a new state.
 * This is the primary (and sole) mechanism to ensure the evolution of
 * the state kept within the Redux state container.
 *
 * A selector is a function retrieving some part of the state and
 * "shaping" it as needed for use by the application.
 */

/**
 * Reducers can be combined in a "tree"-like way.
 * The RootReducer sits at the top, combining all the reducers.
 */
export { default as keyPairReducer } from './KeyPairReducer';
export { default as messageReducer } from './MessageReducer';
export * from './KeyPairReducer';
export * from './MessageReducer';
