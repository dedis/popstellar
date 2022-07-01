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

/**
 * Class Component implementing a react error boundary
 * (https://reactjs.org/docs/error-boundaries.html) The component renders its
 * children unless an error of type NoCurrentLaoError is thrown in which case,
 * the component then displays an error string. This component was created to
 * fix issue #1132 (https://github.com/dedis/popstellar/issues/1132)
 */
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
