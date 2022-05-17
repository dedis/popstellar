import React, { useState } from 'react';
import { TextInput, TextStyle } from 'react-native';
import { CheckBox } from 'react-native-elements';

import { QRCode, TextBlock } from 'core/components';
import { KeyPairStore } from 'core/keypair';
import { Colors, Spacing, Typography } from 'core/styles';
import STRINGS from 'resources/strings';

/**
 * Manage the Identity screen. A user may decide to participate anonymously to a
 * LAO or share personal information
 *
 */

const placeholderBasic: string[] = [
  STRINGS.identity_name_placeholder,
  STRINGS.identity_title_placeholder,
  STRINGS.identity_organization_placeholder,
];

const Identity = () => {
  const [toggleAnonymity, setToggleAnonymity] = useState(true);

  const buildBasicTextInput = (placeholder: string): JSX.Element => (
    <TextInput
      style={[
        { ...Typography.baseCentered, marginBottom: Spacing.xs } as TextStyle,
        { color: toggleAnonymity ? Colors.gray : Colors.black },
      ]}
      key={`input-${placeholder}`}
      placeholder={placeholder}
      editable={!toggleAnonymity}
    />
  );

  const buildEmailTextInput = (placeholder: string): JSX.Element =>
    React.cloneElement(buildBasicTextInput(placeholder), {
      autoCompleteType: 'email',
      keyboardType: 'email-address',
    });

  const buildPhoneTextInput = (placeholder: string): JSX.Element =>
    React.cloneElement(buildBasicTextInput(placeholder), {
      autoCompleteType: 'tel',
      dataDetectorTypes: 'phoneNumber',
      keyboardType: 'phone-pad',
    });

  return (
    <>
      <CheckBox
        checked={toggleAnonymity}
        onPress={() => setToggleAnonymity(!toggleAnonymity)}
        title={STRINGS.identity_check_box_anonymous}
      />
      <TextBlock text={STRINGS.identity_check_box_anonymous_description} />
      {placeholderBasic.map((p: string) => buildBasicTextInput(p))}
      {buildEmailTextInput(STRINGS.identity_email_placeholder)}
      {buildPhoneTextInput(STRINGS.identity_phone_placeholder)}
      <TextBlock text={STRINGS.identity_qrcode_description} visibility={!toggleAnonymity} />
      <QRCode value={KeyPairStore.getPublicKey().toString()} visibility={!toggleAnonymity} />
    </>
  );
};

export default Identity;
