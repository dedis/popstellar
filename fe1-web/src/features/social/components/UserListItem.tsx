import { CompositeScreenProps } from '@react-navigation/core';
import { useNavigation } from '@react-navigation/native';
import { StackScreenProps } from '@react-navigation/stack';
import PropTypes from 'prop-types';
import React, { useState } from 'react';
import { StyleSheet, Text, TextStyle, View, ViewStyle } from 'react-native';
import { useToast } from 'react-native-toast-notifications';
import { useDispatch } from 'react-redux';

import { ProfileIcon, PoPTextButton } from 'core/components';
import { AppParamList } from 'core/navigation/typing/AppParamList';
import { LaoParamList } from 'core/navigation/typing/LaoParamList';
import { SocialParamList } from 'core/navigation/typing/SocialParamList';
import { SocialSearchParamList } from 'core/navigation/typing/SocialSearchParamList';
import { subscribeToChannel } from 'core/network';
import { getUserSocialChannel, Hash, PublicKey } from 'core/objects';
import { gray } from 'core/styles/color';
import { FOUR_SECONDS } from 'resources/const';
import STRINGS from 'resources/strings';

/**
 * Component that shows a user's profile picture, his public key and two buttons:
 * - Follow, which subscribes to the user's social channel.
 * - Profile, that navigates to the user's profile page.
 *
 * @remarks
 * For now, it is not possible to unfollow users, and you have to follow someone to access to
 * their profile. (2021-12-20, Xelowak)
 */
const styles = StyleSheet.create({
  container: {
    borderColor: gray,
    borderTopWidth: 0,
    borderWidth: 1,
    flexDirection: 'row',
    padding: 10,
    width: 600,
  } as ViewStyle,
  leftView: {
    width: 60,
  } as ViewStyle,
  publicKeyText: {
    fontSize: 18,
    fontWeight: '600',
  } as TextStyle,
  rightView: {
    flexDirection: 'column',
    width: 540,
  } as ViewStyle,
  buttonsView: {
    flexDirection: 'row',
  } as ViewStyle,
  buttonView: {
    flex: 1,
  } as ViewStyle,
});

type NavigationProps = CompositeScreenProps<
  StackScreenProps<SocialSearchParamList, typeof STRINGS.social_media_navigation_tab_attendee_list>,
  CompositeScreenProps<
    StackScreenProps<SocialParamList, typeof STRINGS.social_media_navigation_tab_search>,
    CompositeScreenProps<
      StackScreenProps<LaoParamList, typeof STRINGS.navigation_social_media>,
      StackScreenProps<AppParamList, typeof STRINGS.navigation_app_lao>
    >
  >
>;

const UserListItem = (props: IPropTypes) => {
  const [isFollowing, setIsFollowing] = useState(false);
  const { currentUserPublicKey, laoId, publicKey } = props;

  const navigation = useNavigation<NavigationProps['navigation']>();
  const toast = useToast();

  const dispatch = useDispatch();

  const followUser = () => {
    subscribeToChannel(laoId, dispatch, getUserSocialChannel(laoId, publicKey)).catch((error) => {
      console.error(
        `Could not subscribe to channel of user ${publicKey.valueOf()}, error: ${error}`,
      );
      toast.show(`Could not subscribe to channel of user ${publicKey.valueOf()}, error: ${error}`, {
        type: 'danger',
        placement: 'top',
        duration: FOUR_SECONDS,
      });
    });
    setIsFollowing(true);
  };

  const goToUserProfile = () => {
    navigation.navigate(STRINGS.social_media_navigation_tab_user_profile, {
      currentUserPublicKey: currentUserPublicKey,
      userPublicKey: publicKey,
    });
  };

  return (
    <View style={styles.container}>
      <View style={styles.leftView}>
        <ProfileIcon publicKey={publicKey} />
      </View>
      <View style={styles.rightView}>
        <Text style={styles.publicKeyText}>{publicKey.valueOf()}</Text>
        <View style={styles.buttonsView}>
          <View style={styles.buttonView}>
            <PoPTextButton onPress={followUser} disabled={isFollowing}>
              {STRINGS.follow_button}
            </PoPTextButton>
          </View>
          <View style={styles.buttonView}>
            <PoPTextButton onPress={goToUserProfile} disabled={!isFollowing}>
              {STRINGS.profile_button}
            </PoPTextButton>
          </View>
        </View>
      </View>
    </View>
  );
};

const propTypes = {
  laoId: PropTypes.instanceOf(Hash).isRequired,
  publicKey: PropTypes.instanceOf(PublicKey).isRequired,
  currentUserPublicKey: PropTypes.instanceOf(PublicKey).isRequired,
};

UserListItem.prototype = propTypes;

type IPropTypes = {
  laoId: Hash;
  publicKey: PublicKey;
  currentUserPublicKey: PublicKey;
};

export default UserListItem;
