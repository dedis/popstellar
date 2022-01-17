import * as React from 'react';
import PropTypes from 'prop-types';
import { PublicKey } from 'model/objects';
import TextBlock from 'components/TextBlock';
import { StyleSheet, View, ViewStyle } from 'react-native';
import STRINGS from 'res/strings';
import SocialUserProfile from './SocialUserProfile';

const styles = StyleSheet.create({
  textUnavailableView: {
    alignSelf: 'center',
    width: 600,
    marginTop: 20,
  } as ViewStyle,
});

/**
 * UI for the profile of the current user.
 */
const SocialProfile = (props: IPropTypes) => {
  const { currentUserPublicKey } = props;
  if (currentUserPublicKey.valueOf() !== '') {
    return <SocialUserProfile userPublicKey={currentUserPublicKey} />;
  }
  return (
    <View style={styles.textUnavailableView}>
      <TextBlock text={STRINGS.social_media_your_profile_unavailable} />
    </View>
  );
};

const propTypes = {
  currentUserPublicKey: PropTypes.instanceOf(PublicKey).isRequired,
};

SocialProfile.prototype = propTypes;

type IPropTypes = {
  currentUserPublicKey: PublicKey,
};

export default SocialProfile;
