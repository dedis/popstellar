import { CompositeScreenProps } from '@react-navigation/core';
import { useNavigation } from '@react-navigation/native';
import { StackScreenProps } from '@react-navigation/stack';
import { ListItem } from '@rneui/themed';
import PropTypes from 'prop-types';
import React from 'react';
import { StyleSheet, View, ViewStyle } from 'react-native';

import { ProfileIcon } from 'core/components';
import { AppParamList } from 'core/navigation/typing/AppParamList';
import { LaoParamList } from 'core/navigation/typing/LaoParamList';
import { SocialParamList } from 'core/navigation/typing/social/SocialParamList';
import { SocialSearchParamList } from 'core/navigation/typing/social/SocialSearchParamList';
import { PublicKey } from 'core/objects';
import { List, Spacing, Typography } from 'core/styles';
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
  leftView: {
    marginRight: Spacing.x1,
    alignSelf: 'flex-start',
  } as ViewStyle,
});

type NavigationProps = CompositeScreenProps<
  StackScreenProps<
    SocialSearchParamList,
    typeof STRINGS.social_media_search_navigation_attendee_list
  >,
  CompositeScreenProps<
    StackScreenProps<SocialParamList, typeof STRINGS.social_media_navigation_tab_search>,
    CompositeScreenProps<
      StackScreenProps<LaoParamList, typeof STRINGS.navigation_social_media>,
      StackScreenProps<AppParamList, typeof STRINGS.navigation_app_lao>
    >
  >
>;

const UserListItem = ({ publicKey, isFirstItem, isLastItem }: IPropTypes) => {
  const navigation = useNavigation<NavigationProps['navigation']>();

  const goToUserProfile = () => {
    navigation.navigate(STRINGS.social_media_search_navigation_user_profile, {
      userPkString: publicKey.valueOf(),
    });
  };

  const listStyle = List.getListItemStyles(isFirstItem, isLastItem);

  return (
    <ListItem
      containerStyle={listStyle}
      style={listStyle}
      bottomDivider
      onPress={goToUserProfile}
      testID={`user_list_item_${publicKey.toString()}`}>
      <View style={[List.icon, styles.leftView]}>
        <ProfileIcon publicKey={publicKey} />
      </View>
      <ListItem.Content>
        <ListItem.Title style={[Typography.base, Typography.code]} numberOfLines={1}>
          {publicKey.valueOf()}
        </ListItem.Title>
      </ListItem.Content>
      <ListItem.Chevron />
    </ListItem>
  );
};

const propTypes = {
  publicKey: PropTypes.instanceOf(PublicKey).isRequired,
  isFirstItem: PropTypes.bool.isRequired,
  isLastItem: PropTypes.bool.isRequired,
};

UserListItem.prototype = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default UserListItem;
