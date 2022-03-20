import React from 'react';

/**
 * This react context provides the dependencies of the different features to their components
 */
const FeatureContext = React.createContext<{
  [identifier: string]: unknown;
}>({});

export default FeatureContext;
