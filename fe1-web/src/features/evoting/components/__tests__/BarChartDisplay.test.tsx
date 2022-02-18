import React from 'react';
import { render } from '@testing-library/react-native';
import { MajorityResult } from '../../objects';
import BarChartDisplay from '../BarChartDisplay';

describe('BarChartDisplay', () => {
  it('renders correctly', () => {
    const data: MajorityResult[] = [
      { ballotOption: 'Yes', count: 10 },
      { ballotOption: 'No', count: 5 },
      { ballotOption: "I don't know", count: 4 },
    ];
    const component = render(<BarChartDisplay data={data} />).toJSON();
    expect(component).toMatchSnapshot();
  });
});
