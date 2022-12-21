import { Ionicons } from '@expo/vector-icons';
import { CompositeScreenProps } from '@react-navigation/core';
import { useNavigation } from '@react-navigation/native';
import { StackScreenProps } from '@react-navigation/stack';
import PropTypes from 'prop-types';
import React from 'react';
import { Pressable, View } from 'react-native';

import { AppParamList } from 'core/navigation/typing/AppParamList';
import { LaoParamList } from 'core/navigation/typing/LaoParamList';
import { SocialParamList } from 'core/navigation/typing/SocialParamList';
import { SocialSearchParamList } from 'core/navigation/typing/SocialSearchParamList';
import STRINGS from 'resources/strings';

type NavigationProps = CompositeScreenProps<
  StackScreenProps<
    SocialSearchParamList,
    typeof STRINGS.social_media_search_navigation_user_profile
  >,
  CompositeScreenProps<
    StackScreenProps<SocialParamList, typeof STRINGS.social_media_navigation_tab_search>,
    CompositeScreenProps<
      StackScreenProps<LaoParamList, typeof STRINGS.navigation_social_media>,
      StackScreenProps<AppParamList, typeof STRINGS.navigation_app_lao>
    >
  >
>;

/**
 * UI button to go back in navigation. You can specify the string corresponding to where it should
 * navigate along with its size in its properties.
 */
const BackButton = (props: IPropTypes) => {
  const { navigationTabName, size, testID } = props;

  const navigation = useNavigation<NavigationProps['navigation']>();

  return (
    // The view is there to set the button's clickable layout correctly.
    <View style={{ width: size }}>
      <Pressable onPress={() => navigation.navigate(navigationTabName)} testID={testID}>
        <Ionicons name="chevron-back-outline" size={size} />
      </Pressable>
    </View>
  );
};

const propTypes = {
  navigationTabName: PropTypes.string.isRequired,
  size: PropTypes.number,
  testID: PropTypes.string,
};

type IPropTypes = {
  navigationTabName: typeof STRINGS.social_media_search_navigation_attendee_list;
  size: number;
  testID: string;
};

BackButton.defaultProps = {
  size: 23,
  testID: 'backButton',
};

BackButton.propTypes = propTypes;

export default BackButton;
