import { CompositeScreenProps, useNavigation } from '@react-navigation/core';
import { StackScreenProps } from '@react-navigation/stack';
import { ListItem } from '@rneui/themed';
import PropTypes from 'prop-types';
import React, { useContext, useMemo } from 'react';
import { StyleSheet, Text, TextStyle, View, ViewStyle } from 'react-native';
import { useToast } from 'react-native-toast-notifications';
import { useSelector } from 'react-redux';
import TimeAgo from 'react-timeago';

import { ProfileIcon } from 'core/components';
import PoPIconButton from 'core/components/PoPIconButton';
import PoPTouchableOpacity from 'core/components/PoPTouchableOpacity';
import { ActionSheetOption, useActionSheet } from 'core/hooks/ActionSheet';
import { AppParamList } from 'core/navigation/typing/AppParamList';
import { LaoParamList } from 'core/navigation/typing/LaoParamList';
import {
  SocialHomeParamList,
  SocialParamList,
  SocialProfileParamList,
  SocialSearchParamList,
  SocialTopChirpsParamList,
} from 'core/navigation/typing/social';
import { List, Spacing, Typography } from 'core/styles';
import STRINGS from 'resources/strings';

import { SocialMediaContext } from '../context';
import { SocialHooks } from '../hooks';
import { requestAddReaction, requestDeleteChirp } from '../network/SocialMessageApi';
import { Chirp } from '../objects';
import { makeHasReactedSelector, makeReactionCountsSelector } from '../reducer';

type NavigationProps = CompositeScreenProps<
  CompositeScreenProps<
    StackScreenProps<
      | SocialHomeParamList
      | SocialTopChirpsParamList
      | SocialSearchParamList
      | SocialProfileParamList,
      /* there is probably a better way to type this, this component can appear in any screen of the above navigators */
      any
    >,
    StackScreenProps<
      SocialParamList,
      | typeof STRINGS.social_media_navigation_tab_home
      | typeof STRINGS.social_media_navigation_tab_top_chirps
      | typeof STRINGS.social_media_navigation_tab_search
      | typeof STRINGS.social_media_navigation_tab_profile
    >
  >,
  CompositeScreenProps<
    StackScreenProps<LaoParamList, typeof STRINGS.navigation_social_media>,
    StackScreenProps<AppParamList, typeof STRINGS.navigation_app_lao>
  >
>;

/**
 * Component to display a chirp
 */

const styles = StyleSheet.create({
  profileIcon: {
    alignSelf: 'flex-start',
  } as ViewStyle,
  reactionsView: {
    width: '100%',
    flexDirection: 'row',
    flexWrap: 'wrap',
  } as ViewStyle,
  reactionView: {
    flexDirection: 'row',
    alignItems: 'center',
    marginTop: Spacing.x1,
    marginRight: Spacing.x1,
  } as ViewStyle,
  reactionCounter: {
    marginLeft: Spacing.x05,
  } as TextStyle,
  chirpButtonSpacer: {
    flexGrow: 1,
  },
  chirpTimeContainer: {
    marginTop: Spacing.x1,
    alignSelf: 'flex-end',
    marginLeft: 'auto',
  } as ViewStyle,
});

const FOUR_SECONDS = 4000;

const ChirpCard = ({ chirp, isFirstItem, isLastItem }: IPropTypes) => {
  const toast = useToast();
  const laoId = SocialHooks.useCurrentLaoId();
  const isConnected = SocialHooks.useConnectedToLao();
  const navigation = useNavigation<NavigationProps['navigation']>();

  const { currentUserPopTokenPublicKey } = useContext(SocialMediaContext);

  if (laoId === undefined) {
    throw new Error('Impossible to render chirp, current lao id is undefined');
  }

  const selectReactionList = useMemo(
    () => makeReactionCountsSelector(laoId, chirp.id),
    [laoId, chirp.id],
  );
  const reactions = useSelector(selectReactionList);

  const selectHasReacted = useMemo(
    () => makeHasReactedSelector(laoId, chirp.id, currentUserPopTokenPublicKey),
    [laoId, chirp.id, currentUserPopTokenPublicKey],
  );
  const hasReacted = useSelector(selectHasReacted);

  const thumbsUp = reactions['👍'];
  const thumbsDown = reactions['👎'];
  const heart = reactions['❤️'];

  const reactionsDisabled = {
    '👍': !isConnected || !currentUserPopTokenPublicKey || hasReacted['👍'],
    '👎': !isConnected || !currentUserPopTokenPublicKey || hasReacted['👎'],
    '❤️': !isConnected || !currentUserPopTokenPublicKey || hasReacted['❤️'],
  };

  const showActionSheet = useActionSheet();

  const addReaction = (reaction_codepoint: string) => {
    requestAddReaction(reaction_codepoint, chirp.id, laoId).catch((err) => {
      toast.show(`Could not add reaction, error: ${err}`, {
        type: 'danger',
        placement: 'bottom',
        duration: FOUR_SECONDS,
      });
    });
  };

  // TODO: delete a chirp posted with a PoP token from a previous roll call.
  const isSender =
    currentUserPopTokenPublicKey &&
    currentUserPopTokenPublicKey.valueOf() === chirp.sender.valueOf();

  const deleteChirp = () => {
    if (!currentUserPopTokenPublicKey) {
      return;
    }

    requestDeleteChirp(currentUserPopTokenPublicKey, chirp.id, laoId).catch((err) => {
      toast.show(`Could not remove chirp, error: ${err}`, {
        type: 'danger',
        placement: 'bottom',
        duration: FOUR_SECONDS,
      });
    });
  };

  const listStyle = List.getListItemStyles(isFirstItem, isLastItem);

  const actionSheetOptions: ActionSheetOption[] = [];
  if (isSender && !chirp.isDeleted && isConnected) {
    actionSheetOptions.push({
      displayName: STRINGS.social_media_delete_chirp,
      action: deleteChirp,
    });
  }

  return (
    <ListItem containerStyle={listStyle} style={listStyle} bottomDivider>
      <PoPTouchableOpacity
        onPress={() =>
          navigation.navigate(STRINGS.social_media_navigation_user_profile, {
            userPkString: chirp.sender.valueOf(),
          })
        }>
        <View style={[List.icon, styles.profileIcon]}>
          <ProfileIcon publicKey={chirp.sender} />
        </View>
      </PoPTouchableOpacity>
      <ListItem.Content>
        <ListItem.Title
          style={[Typography.base, Typography.small, Typography.inactive]}
          numberOfLines={1}>
          {chirp.sender.valueOf()}
        </ListItem.Title>
        <ListItem.Subtitle>
          {chirp.isDeleted ? (
            <Text style={[Typography.base, Typography.inactive]}>{STRINGS.deleted_chirp}</Text>
          ) : (
            <Text style={Typography.base}>{chirp.text}</Text>
          )}
        </ListItem.Subtitle>
        <View style={styles.reactionsView}>
          {!chirp.isDeleted && (
            <>
              <View style={styles.reactionView}>
                <PoPIconButton
                  name="thumbsUp"
                  testID="thumbs-up"
                  onPress={() => addReaction('👍')}
                  disabled={reactionsDisabled['👍']}
                  size="small"
                  toolbar
                />
                <Text style={[Typography.base, Typography.small, styles.reactionCounter]}>
                  {thumbsUp}
                </Text>
              </View>
              <View style={styles.reactionView}>
                <PoPIconButton
                  name="thumbsDown"
                  testID="thumbs-down"
                  onPress={() => addReaction('👎')}
                  disabled={reactionsDisabled['👎']}
                  size="small"
                  toolbar
                />
                <Text style={[Typography.base, Typography.small, styles.reactionCounter]}>
                  {thumbsDown}
                </Text>
              </View>
              <View style={styles.reactionView}>
                <PoPIconButton
                  name="heart"
                  testID="heart"
                  onPress={() => addReaction('❤️')}
                  disabled={reactionsDisabled['❤️']}
                  size="small"
                  toolbar
                />
                <Text style={[Typography.base, Typography.small, styles.reactionCounter]}>
                  {heart}
                </Text>
              </View>
              {actionSheetOptions.length > 0 && (
                <View style={styles.reactionView}>
                  <PoPIconButton
                    name="options"
                    testID="chirp_action_options"
                    onPress={() => showActionSheet(actionSheetOptions)}
                    size="small"
                    toolbar
                  />
                </View>
              )}
            </>
          )}
          <View style={styles.chirpButtonSpacer} />
          <View style={styles.chirpTimeContainer}>
            <Text style={[Typography.base, Typography.small]}>
              <TimeAgo date={chirp.time.valueOf() * 1000} />
            </Text>
          </View>
        </View>
      </ListItem.Content>
    </ListItem>
  );
};

const propTypes = {
  chirp: PropTypes.instanceOf(Chirp).isRequired,
  isFirstItem: PropTypes.bool.isRequired,
  isLastItem: PropTypes.bool.isRequired,
};

ChirpCard.prototype = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default ChirpCard;
