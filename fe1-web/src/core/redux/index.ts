/**
 * The redux module contains the basic configuration of Redux for the project.
 * Redux allows to manage state by using reducers.
 *
 * A reducer (within the Redux framework) is nothing more than a function
 * taking the current state, an action, and returning a new state.
 * This is the primary (and sole) mechanism to ensure the evolution of
 * the state kept within the Redux state container.
 *
 * Reducers can be combined in a "tree"-like way.
 * The RootReducer sits at the top, combining all the reducers.
 *
 * A selector is a function retrieving some part of the state and
 * "shaping" it as needed for use by the application.
 */

export * from './GlobalStore';
export * from './Manage';
