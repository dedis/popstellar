import { describe } from '@jest/globals';
import { render } from '@testing-library/react-native';
import React from 'react';

import ParagraphBlock from '../ParagraphBlock';

describe('ParagraphBlock', () => {
  it('renders correctly', () => {
    const component = render(<ParagraphBlock text="Some text" />).toJSON();
    expect(component).toMatchSnapshot();
  });
  it('renders correctly using bold prop', () => {
    const component = render(<ParagraphBlock bold text="Some text" />).toJSON();
    expect(component).toMatchSnapshot();
  });
});
