import { CompositeScreenProps } from '@react-navigation/core';
import { useNavigation } from '@react-navigation/native';
import { StackScreenProps } from '@react-navigation/stack';
import { ListItem } from '@rneui/themed';
import PropTypes from 'prop-types';
import React, { useState } from 'react';
import { StyleSheet, View, ViewStyle } from 'react-native';
import { useToast } from 'react-native-toast-notifications';
import { useDispatch } from 'react-redux';

import { PoPTextButton, ProfileIcon } from 'core/components';
import { AppParamList } from 'core/navigation/typing/AppParamList';
import { LaoParamList } from 'core/navigation/typing/LaoParamList';
import { SocialParamList } from 'core/navigation/typing/SocialParamList';
import { SocialSearchParamList } from 'core/navigation/typing/SocialSearchParamList';
import { subscribeToChannel } from 'core/network';
import { getUserSocialChannel, Hash, PublicKey } from 'core/objects';
import { List, Spacing, Typography } from 'core/styles';
import { FOUR_SECONDS } from 'resources/const';
import STRINGS from 'resources/strings';

import { SocialHooks } from '../hooks';

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
  leftView: {
    marginRight: Spacing.x1,
    alignSelf: 'flex-start',
  } as ViewStyle,
  buttonsView: {
    marginTop: Spacing.x1,
    flexDirection: 'row',
  } as ViewStyle,
  buttonView: {
    marginRight: Spacing.x1,
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

const UserListItem = ({ laoId, publicKey, isFirstItem, isLastItem }: IPropTypes) => {
  const [isFollowing, setIsFollowing] = useState(false);
  const isConnected = SocialHooks.useConnectedToLao();

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
      userPkString: publicKey.valueOf(),
    });
  };

  const listStyle = List.getListItemStyles(isFirstItem, isLastItem);

  return (
    <ListItem containerStyle={listStyle} style={listStyle} bottomDivider>
      <View style={[List.icon, styles.leftView]}>
        <ProfileIcon publicKey={publicKey} />
      </View>
      <ListItem.Content>
        <ListItem.Title style={Typography.base} numberOfLines={1}>
          {publicKey.valueOf()}
        </ListItem.Title>
        <View style={styles.buttonsView}>
          <View style={styles.buttonView}>
            <PoPTextButton onPress={followUser} disabled={isFollowing || !isConnected} toolbar>
              {STRINGS.follow_button}
            </PoPTextButton>
          </View>
          <View style={styles.buttonView}>
            <PoPTextButton onPress={goToUserProfile} toolbar>
              {STRINGS.profile_button}
            </PoPTextButton>
          </View>
        </View>
      </ListItem.Content>
    </ListItem>
  );
};

const propTypes = {
  laoId: PropTypes.instanceOf(Hash).isRequired,
  publicKey: PropTypes.instanceOf(PublicKey).isRequired,
  isFirstItem: PropTypes.bool.isRequired,
  isLastItem: PropTypes.bool.isRequired,
};

UserListItem.prototype = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default UserListItem;
