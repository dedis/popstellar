import { describe } from '@jest/globals';
import { render } from '@testing-library/react-native';
import React from 'react';
import { Provider } from 'react-redux';
import { combineReducers, createStore } from 'redux';

import MockNavigator from '__tests__/components/MockNavigator';
import { mockLao } from '__tests__/utils';
import { addLao, laoReducer } from 'features/lao/reducer';

import LaoList from '../LaoList';

describe('LaoList', () => {
  it('renders correctly with no item', () => {
    const mockStore = createStore(combineReducers(laoReducer));

    const component = render(
      <Provider store={mockStore}>
        <MockNavigator component={LaoList} />
      </Provider>,
    ).toJSON();
    expect(component).toMatchSnapshot();
  });

  it('renders correctly with one item', () => {
    const mockStore = createStore(combineReducers(laoReducer));
    mockStore.dispatch(addLao(mockLao.toState()));

    const component = render(
      <Provider store={mockStore}>
        <MockNavigator component={LaoList} />
      </Provider>,
    ).toJSON();
    expect(component).toMatchSnapshot();
  });
});
