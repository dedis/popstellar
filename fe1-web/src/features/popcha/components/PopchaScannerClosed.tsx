import PropTypes from 'prop-types';
import React from 'react';
import { StyleSheet, Text, View, ViewStyle } from 'react-native';

import PoPTouchableOpacity from 'core/components/PoPTouchableOpacity';
import QrCodeScanner, { QrCodeScannerUIElementContainer } from 'core/components/QrCodeScanner';
import { Spacing, Typography } from 'core/styles';
import STRINGS from 'resources/strings';

import { PopchaHooks } from '../hooks';

const styles = StyleSheet.create({
  topMargin: {
    marginTop: Spacing.x05,
  } as ViewStyle,
});

const PopchaScannerClosed = ({ containerStyle, onOpenPress }: IPropTypes) => {
  const laoId = PopchaHooks.useCurrentLaoId();

  return (
    <>
      <QrCodeScanner showCamera={false} handleScan={() => {}}>
        <View style={containerStyle}>
          <View>
            <Text style={Typography.paragraph}>
              {STRINGS.popcha_display_current_lao}
              {laoId}
            </Text>
          </View>
          <View>
            <View style={[QrCodeScannerUIElementContainer, styles.topMargin]}>
              <PoPTouchableOpacity testID="popcha_scanner_button" onPress={onOpenPress}>
                <Text style={[Typography.base, Typography.accent]}>
                  {STRINGS.popcha_open_scanner}
                </Text>
              </PoPTouchableOpacity>
            </View>
          </View>
        </View>
      </QrCodeScanner>
    </>
  );
};

const propTypes = {
  // eslint-disable-next-line react/forbid-prop-types
  containerStyle: PropTypes.any,
  onOpenPress: PropTypes.func.isRequired,
};

PopchaScannerClosed.propTypes = propTypes;
PopchaScannerClosed.defaultProps = {
  containerStyle: {},
};

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default PopchaScannerClosed;
