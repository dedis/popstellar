import PropTypes from 'prop-types';
import React from 'react';
import { Text, View } from 'react-native';

import PoPTouchableOpacity from 'core/components/PoPTouchableOpacity';
import QrCodeScanner, { QrCodeScannerUIElementContainer } from 'core/components/QrCodeScanner';
import { Typography } from 'core/styles';
import STRINGS from 'resources/strings';

import { PopchaHooks } from '../hooks';

const PopchaScannerClosed = ({ containerStyle, onOpenPress, topMarginStyle }: IPropTypes) => {
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
            <View style={[QrCodeScannerUIElementContainer, topMarginStyle]}>
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
  // eslint-disable-next-line react/forbid-prop-types
  topMarginStyle: PropTypes.any,
  onOpenPress: PropTypes.func.isRequired,
};

PopchaScannerClosed.propTypes = propTypes;
PopchaScannerClosed.defaultProps = {
  topMarginStyle: {},
  containerStyle: {},
};

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default PopchaScannerClosed;
