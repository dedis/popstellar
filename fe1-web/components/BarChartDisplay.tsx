import React from 'react';
import PropTypes from 'prop-types';
import { View } from 'react-native';
import styleContainer from 'styles/stylesheets/container';
import { BarChart } from 'react-native-chart-kit';

/**
 * Creates and displays a QR code with the public key of the current user
 */

const BarChartDisplay = (props: IPropTypes) => {
  const { data } = props;

  const data2 = {
    labels: ['January', 'February', 'March', 'April', 'May', 'June'],
    datasets: [
      {
        data: [20, 45, 28, 80, 99, 43],
      },
    ],
  };

  const chartConfig = {
    backgroundColor: '#000000',
    backgroundGradientFrom: '#fffbf6',
    backgroundGradientTo: '#fffbf6',
    color: (opacity = 1) => `rgba(0, 0, 0, ${opacity})`,
    labelColor: (opacity = 1) => `rgba(0, 0, 0, ${opacity})`,
    style: { borderRadius: 16 },
    propsForDots: {
      r: '6',
      strokeWidth: '2',
      stroke: '#000000',
    },
  };

  // Displays a Bar Chart
  return (
    <View style={[styleContainer.anchoredCenter, { padding: 10, justifyContent: 'flex-start' }]}>
      <BarChart data={data2} width={460} height={260} chartConfig={chartConfig} />
    </View>
  );
};

const propTypes = {
  data: PropTypes.arrayOf(PropTypes.number).isRequired,
};
BarChartDisplay.propTypes = propTypes;

type IPropTypes = {
  data: number[]
};

export default BarChartDisplay;
