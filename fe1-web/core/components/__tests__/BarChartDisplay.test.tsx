import React from 'react';
import { render } from '@testing-library/react-native';
import { MajorityResult } from 'model/objects';
import BarChartDisplay from 'core/components/BarChartDisplay';

describe('BarChartDisplay', () => {
  it('renders correctly', () => {
    const data: MajorityResult[] = [
      { ballot_option: 'Yes', count: 10 },
      { ballot_option: 'No', count: 5 },
      { ballot_option: 'I don\'t know', count: 4 },
    ];
    const component = render(<BarChartDisplay data={data} />).toJSON();
    expect(component).toMatchSnapshot();
  });
});
