import PropTypes, { shape } from 'prop-types';
import React from 'react';
import { StyleSheet, View, ViewStyle } from 'react-native';
import { BarChart } from 'react-native-chart-kit';

import { MajorityResult } from '../objects';

const styles = StyleSheet.create({
  barChartView: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'flex-start',
    padding: 10,
  } as ViewStyle,
});

/**
 * Displays a simple bar chart
 */
const BarChartDisplay = (props: IPropTypes) => {
  const { data } = props;

  const data2 = {
    labels: data.map((d) => d.ballotOption),
    datasets: [
      {
        data: data.map((d) => d.count),
      },
    ],
  };

  const chartConfig = {
    backgroundGradientFrom: '#1E2923',
    backgroundGradientFromOpacity: 0,
    backgroundGradientTo: '#08130D',
    backgroundGradientToOpacity: 0.3,
    height: 5000,
    color: (opacity = 1) => `rgba(0, 0, 0, ${opacity})`,
    strokeWidth: 2, // optional, default 3
    decimalPlaces: 0,
    useShadowColorFromDataset: false, // optional
  };

  // Displays a Bar Chart
  return (
    <View style={styles.barChartView}>
      <BarChart
        data={data2}
        width={460}
        height={260}
        chartConfig={chartConfig}
        yAxisLabel=""
        yAxisSuffix=""
      />
    </View>
  );
};

const propTypes = {
  data: PropTypes.arrayOf(
    shape({
      ballotOption: PropTypes.string.isRequired,
      count: PropTypes.number.isRequired,
    }),
  ).isRequired,
};
BarChartDisplay.propTypes = propTypes;

type IPropTypes = {
  data: MajorityResult[];
};

export default BarChartDisplay;
