import React, { useState } from 'react';
import { StyleSheet, Text } from 'react-native';
import { CheckBox } from 'react-native-elements';

import { Input, QRCode } from 'core/components';
import { KeyPairStore } from 'core/keypair';
import { Color, Spacing, Typography } from 'core/styles';
import STRINGS from 'resources/strings';

/**
 * Manage the Identity screen. A user may decide to participate anonymously to a
 * LAO or share personal information
 */

const styles = StyleSheet.create({
  checkboxContainer: {
    padding: Spacing.x05,
    marginLeft: 0,
    marginRight: 0,
    backgroundColor: Color.transparent,
    borderWidth: 0,
  },
  checkbox: {
    padding: 0,
  },
});

const Identity = () => {
  const [isAnonymous, setIsAnonymous] = useState(true);

  const [name, setName] = useState('');
  const [title, setTitle] = useState('');
  const [organization, setOrganization] = useState('');
  const [email, setEmail] = useState('');
  const [phoneNumber, setPhoneNumber] = useState('');

  return (
    <>
      <CheckBox
        containerStyle={styles.checkboxContainer}
        style={styles.checkbox}
        checked={isAnonymous}
        onPress={() => setIsAnonymous(!isAnonymous)}
        title={STRINGS.identity_check_box_anonymous}
      />
      <Text style={Typography.paragraph}>{STRINGS.identity_check_box_anonymous_description}</Text>

      <Text style={Typography.paragraph}>{STRINGS.identity_check_box_anonymous_description}</Text>

      <Text style={[Typography.paragraph, Typography.important]}>
        {STRINGS.identity_name_label}
      </Text>
      <Input
        value={name}
        onChange={setName}
        placeholder={STRINGS.identity_name_placeholder}
        enabled={!isAnonymous}
      />

      <Text style={[Typography.paragraph, Typography.important]}>
        {STRINGS.identity_title_label}
      </Text>
      <Input
        value={title}
        onChange={setTitle}
        placeholder={STRINGS.identity_title_placeholder}
        enabled={!isAnonymous}
      />

      <Text style={[Typography.paragraph, Typography.important]}>
        {STRINGS.identity_organization_label}
      </Text>
      <Input
        value={organization}
        onChange={setOrganization}
        placeholder={STRINGS.identity_organization_placeholder}
        enabled={!isAnonymous}
      />

      <Text style={[Typography.paragraph, Typography.important]}>
        {STRINGS.identity_email_label}
      </Text>
      <Input
        value={email}
        onChange={setEmail}
        placeholder={STRINGS.identity_email_placeholder}
        enabled={!isAnonymous}
      />

      <Text style={[Typography.paragraph, Typography.important]}>
        {STRINGS.identity_phone_label}
      </Text>
      <Input
        value={phoneNumber}
        onChange={setPhoneNumber}
        placeholder={STRINGS.identity_phone_placeholder}
        enabled={!isAnonymous}
      />

      {!isAnonymous && (
        <>
          <Text style={[Typography.paragraph, Typography.important]}>
            {STRINGS.identity_qrcode_description}
          </Text>
          <QRCode value={KeyPairStore.getPublicKey().toString()} visibility={!isAnonymous} />
        </>
      )}
    </>
  );
};

export default Identity;
