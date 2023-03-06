import { CompositeScreenProps, useNavigation } from '@react-navigation/core';
import { StackScreenProps } from '@react-navigation/stack';
import { ListItem } from '@rneui/themed';
import PropTypes from 'prop-types';
import React, { useContext, useMemo, useState } from 'react';
import { StyleSheet, Text, TextStyle, View, ViewStyle } from 'react-native';
import { useToast } from 'react-native-toast-notifications';
import { useSelector } from 'react-redux';
import TimeAgo from 'react-timeago';

import { ProfileIcon, ConfirmModal } from 'core/components';
import PoPIconButton from 'core/components/PoPIconButton';
import PoPTouchableOpacity from 'core/components/PoPTouchableOpacity';
import { AppParamList } from 'core/navigation/typing/AppParamList';
import { LaoParamList } from 'core/navigation/typing/LaoParamList';
import { SocialParamList } from 'core/navigation/typing/social';
import { Hash } from 'core/objects';
import { List, Spacing, Typography } from 'core/styles';
import STRINGS from 'resources/strings';

import { SocialMediaContext } from '../context';
import { SocialHooks } from '../hooks';
import {
  requestAddReaction,
  requestDeleteChirp,
  requestDeleteReaction,
} from '../network/SocialMessageApi';
import { Chirp } from '../objects';
import { makeReactedSelector, makeReactionCountsSelector } from '../reducer';

type NavigationProps = CompositeScreenProps<
  StackScreenProps<
    SocialParamList,
    | typeof STRINGS.social_media_navigation_tab_home
    | typeof STRINGS.social_media_navigation_tab_top_chirps
    | typeof STRINGS.social_media_navigation_tab_search
    | typeof STRINGS.social_media_navigation_tab_profile
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
  senderPrefix: {
    marginTop: Spacing.x05,
  },
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

  const selectReacted = useMemo(
    () => makeReactedSelector(laoId, chirp.id, currentUserPopTokenPublicKey),
    [laoId, chirp.id, currentUserPopTokenPublicKey],
  );
  const reacted = useSelector(selectReacted);

  const thumbsUp = reactions['👍'];
  const thumbsDown = reactions['👎'];
  const heart = reactions['❤️'];

  const reactionsDisabled = {
    '👍': !isConnected || !currentUserPopTokenPublicKey,
    '👎': !isConnected || !currentUserPopTokenPublicKey,
    '❤️': !isConnected || !currentUserPopTokenPublicKey,
  };
  const addReaction = (reaction_codepoint: string) => {
    requestAddReaction(reaction_codepoint, chirp.id, laoId).catch((err) => {
      toast.show(`Could not add reaction, error: ${err}`, {
        type: 'danger',
        placement: 'bottom',
        duration: FOUR_SECONDS,
      });
    });
  };

  const deleteReaction = (reactionId: Hash) => {
    requestDeleteReaction(reactionId, laoId).catch((err) => {
      toast.show(`Could not delete reaction, error: ${err}`, {
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

  const [showDeleteConfirmation, setShowDeleteConfirmation] = useState(false);

  return (
    <ListItem containerStyle={listStyle} style={listStyle} bottomDivider>
      <PoPTouchableOpacity
        onPress={() =>
          navigation.navigate(STRINGS.navigation_app_lao, {
            screen: STRINGS.navigation_social_media,
            params: {
              screen: STRINGS.social_media_navigation_tab_search,
              params: {
                screen: STRINGS.social_media_search_navigation_user_profile,
                params: { userPkString: chirp.sender.valueOf() },
              },
            },
          })
        }>
        <View style={[List.icon, styles.profileIcon]}>
          <ProfileIcon publicKey={chirp.sender} />
          <Text
            style={[
              Typography.base,
              Typography.small,
              Typography.inactive,
              Typography.centered,
              styles.senderPrefix,
            ]}
            numberOfLines={1}
            selectable>
            {chirp.sender.valueOf().slice(0, 4)}
          </Text>
        </View>
      </PoPTouchableOpacity>
      <ListItem.Content>
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
                  onPress={() =>
                    reacted['👍'] ? deleteReaction(reacted['👍'].id) : addReaction('👍')
                  }
                  disabled={reactionsDisabled['👍']}
                  size="small"
                  buttonStyle={reacted['👍'] ? 'primary' : 'secondary'}
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
                  onPress={() =>
                    reacted['👎'] ? deleteReaction(reacted['👎'].id) : addReaction('👎')
                  }
                  disabled={reactionsDisabled['👎']}
                  size="small"
                  buttonStyle={reacted['👎'] ? 'primary' : 'secondary'}
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
                  onPress={() =>
                    reacted['❤️'] ? deleteReaction(reacted['❤️'].id) : addReaction('❤️')
                  }
                  disabled={reactionsDisabled['❤️']}
                  size="small"
                  buttonStyle={reacted['❤️'] ? 'primary' : 'secondary'}
                  toolbar
                />
                <Text style={[Typography.base, Typography.small, styles.reactionCounter]}>
                  {heart}
                </Text>
              </View>
              {isSender && (
                <View style={styles.reactionView}>
                  <PoPIconButton
                    name="delete"
                    testID="delete"
                    onPress={() => setShowDeleteConfirmation(true)}
                    size="small"
                    buttonStyle="secondary"
                    toolbar
                  />
                  <ConfirmModal
                    onConfirmPress={deleteChirp}
                    visibility={showDeleteConfirmation}
                    description={STRINGS.social_media_ask_confirm_delete_chirp}
                    title={STRINGS.social_media_confirm_delete_chirp}
                    setVisibility={setShowDeleteConfirmation}
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
