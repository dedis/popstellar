import { NavigationContainer } from '@react-navigation/native';
import { createStackNavigator } from '@react-navigation/stack';
import React from 'react';

const Stack = createStackNavigator();
const MockNavigator = ({
  component,
  params = {},
  screenOptions = {},
}: {
  component: React.ComponentType<unknown>;
  params?: Partial<object>;
  screenOptions?: Partial<object>;
}) => {
  return (
    <NavigationContainer>
      <Stack.Navigator>
        <Stack.Screen
          name="MockScreen"
          component={component}
          initialParams={params}
          options={screenOptions}
        />
      </Stack.Navigator>
    </NavigationContainer>
  );
};

MockNavigator.defaultProps = {
  params: {},
  screenOptions: {},
};

export default MockNavigator;
