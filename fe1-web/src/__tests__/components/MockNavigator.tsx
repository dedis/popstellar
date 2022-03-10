import React from 'react';
import { NavigationContainer } from '@react-navigation/native';
import { createStackNavigator } from '@react-navigation/stack';

const Stack = createStackNavigator();
const MockNavigator = ({
  component,
  params = {},
}: {
  component: React.ComponentType<unknown>;
  params?: Partial<object>;
}) => {
  return (
    <NavigationContainer>
      <Stack.Navigator>
        <Stack.Screen name="MockScreen" component={component} initialParams={params} />
      </Stack.Navigator>
    </NavigationContainer>
  );
};

MockNavigator.defaultProps = {
  params: {},
};

export default MockNavigator;
