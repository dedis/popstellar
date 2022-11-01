import PropTypes from 'prop-types';
import React from 'react';
import { StyleSheet, Text, View, ViewStyle } from 'react-native';

import { PoPIcon } from 'core/components';
import { Border, Color, Icon, Spacing, Typography } from 'core/styles';
import STRINGS from 'resources/strings';

import { Election, ElectionVersion } from '../objects';

const styles = StyleSheet.create({
  warning: {
    padding: Spacing.x1,
    marginBottom: Spacing.x1,
    borderRadius: Border.radius,

    flexDirection: 'row',
    alignItems: 'center',
  } as ViewStyle,
  warningText: {
    flex: 1,
    marginLeft: Spacing.x1,
  } as ViewStyle,
  openBallot: {
    backgroundColor: Color.warning,
  } as ViewStyle,
  secretBallot: {
    backgroundColor: Color.success,
  } as ViewStyle,
});

const ElectionVersionNotice = ({ election }: IPropTypes) => {
  switch (election.version) {
    case ElectionVersion.OPEN_BALLOT:
      return (
        <View style={[styles.warning, styles.openBallot]}>
          <PoPIcon name="warning" color={Color.contrast} size={Icon.largeSize} />
          <View style={styles.warningText}>
            <Text style={[Typography.base, Typography.important, Typography.negative]}>
              {STRINGS.general_notice}
            </Text>
            <Text style={[Typography.base, Typography.negative]}>
              {STRINGS.election_warning_open_ballot}
            </Text>
          </View>
        </View>
      );
    case ElectionVersion.SECRET_BALLOT:
      return (
        <View style={[styles.warning, styles.secretBallot]}>
          <PoPIcon name="info" color={Color.contrast} size={Icon.largeSize} />
          <View style={styles.warningText}>
            <Text style={[Typography.base, Typography.important, Typography.negative]}>
              {STRINGS.general_notice}
            </Text>
            <Text style={[Typography.base, Typography.negative]}>
              {STRINGS.election_info_secret_ballot}
            </Text>
          </View>
        </View>
      );
    default:
      return null;
  }
};

const propTypes = {
  election: PropTypes.instanceOf(Election).isRequired,
};
ElectionVersionNotice.propTypes = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default ElectionVersionNotice;
