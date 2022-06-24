import React from 'react';
import { Text } from 'react-native';

import STRINGS from 'resources/strings';

import { NoCurrentLaoError } from './NoCurrentLaoError';

interface IProps {
  children: React.ReactNode;
}

interface State {
  hasError: boolean;
}

class NoCurrentLaoErrorBoundary extends React.Component<IProps, State> {
  constructor(props: IProps) {
    super(props);

    this.state = { hasError: false };
  }

  static getDerivedStateFromError(error: Error) {
    // Check if the caught error is the one we want to handle
    if (error instanceof NoCurrentLaoError) {
      // Update state so the next render will show the fallback UI
      return { hasError: true };
    }

    throw error;
  }

  render() {
    const { children } = this.props;
    const { hasError } = this.state;

    if (hasError) {
      return <Text>{STRINGS.lao_no_current}</Text>;
    }

    return children;
  }
}

export default NoCurrentLaoErrorBoundary;
