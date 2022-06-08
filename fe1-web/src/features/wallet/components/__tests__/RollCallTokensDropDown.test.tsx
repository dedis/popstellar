import { render } from '@testing-library/react-native';
import React from 'react';

import { mockKeyPair, mockLao } from '__tests__/utils';
import { Hash, PopToken } from 'core/objects';

import { RollCallToken } from '../../../../core/objects/RollCallToken';
import { RollCallTokensDropDown } from '../index';

const mockToken = PopToken.fromState(mockKeyPair.toState());
const mockArray = [
  new RollCallToken({
    token: mockToken,
    laoId: mockLao.id,
    rollCallId: new Hash('id'),
    rollCallName: 'rcname',
  }),
];

describe('RollCallTokensDropDown', () => {
  it('renders correctly', () => {
    const { toJSON } = render(
      <RollCallTokensDropDown
        rollCallTokens={mockArray}
        selectedTokenIndex={0}
        onIndexChange={() => {}}
      />,
    );
    expect(toJSON()).toMatchSnapshot();
  });
});
