import PropTypes from 'prop-types';
import React, { useContext, useMemo, useState } from 'react';
import { StyleSheet, Text, TextStyle, View, ViewStyle } from 'react-native';
import { useToast } from 'react-native-toast-notifications';
import { useSelector } from 'react-redux';
import TimeAgo from 'react-timeago';

import { ConfirmModal, ProfileIcon } from 'core/components';
import PoPIconButton from 'core/components/PoPIconButton';
import { Typography } from 'core/styles';
import { gray } from 'core/styles/color';
import STRINGS from 'resources/strings';

import { SocialMediaContext } from '../context';
import { SocialHooks } from '../hooks';
import { requestAddReaction, requestDeleteChirp } from '../network/SocialMessageApi';
import { Chirp } from '../objects';
import { makeReactionsList } from '../reducer';

/**
 * Component to display a chirp
 */

const styles = StyleSheet.create({
  container: {
    borderColor: gray,
    borderTopWidth: 0,
    borderWidth: 1,
    flexDirection: 'column',
    padding: 10,
    width: 600,
  } as ViewStyle,
  innerContainer: {
    flexDirection: 'row',
  } as ViewStyle,
  leftView: {
    width: 60,
  } as ViewStyle,
  rightView: {
    display: 'flex',
    flexDirection: 'column',
  } as ViewStyle,
  senderText: {
    fontSize: 18,
    fontWeight: '600',
  } as TextStyle,
  senderView: {
    fontSize: 18,
    marginTop: 7,
  } as ViewStyle,
  chirpText: {
    fontSize: 18,
    paddingBottom: 20,
    paddingTop: 10,
    width: 520,
  } as TextStyle,
  deletedChirpText: {
    fontSize: 18,
    paddingBottom: 20,
    paddingTop: 10,
    width: 520,
    color: gray,
  } as TextStyle,
  reactionsView: {
    flexDirection: 'row',
    fontSize: 18,
  } as ViewStyle,
  reactionView: {
    flexDirection: 'row',
    flex: 1,
    marginRight: 10,
  } as ViewStyle,
  bottomView: {
    flexDirection: 'row',
    display: 'flex',
    marginTop: 10,
  } as ViewStyle,
  deleteChirpContainer: {
    marginRight: 'auto',
  } as ViewStyle,
  chirpTimeContainer: {
    marginLeft: 'auto',
  } as ViewStyle,
});

const FOUR_SECONDS = 4000;

const ChirpCard = ({ chirp }: IPropTypes) => {
  const toast = useToast();
  const laoId = SocialHooks.useCurrentLaoId();
  const isConnected = SocialHooks.useConnectedToLao();
  const { currentUserPopTokenPublicKey } = useContext(SocialMediaContext);

  if (laoId === undefined) {
    throw new Error('Impossible to render chirp, current lao id is undefined');
  }

  const reactionList = useMemo(() => makeReactionsList(laoId.valueOf()), [laoId]);
  const reactions = useSelector(reactionList)[chirp.id.toString()];

  const thumbsUp = reactions ? reactions['👍'] : 0;
  const thumbsDown = reactions ? reactions['👎'] : 0;
  const heart = reactions ? reactions['❤️'] : 0;

  const [deleteModalIsVisible, setDeleteModalIsVisible] = useState(false);

  const addReactionDisabled = !isConnected || !currentUserPopTokenPublicKey;

  const addReaction = (reaction_codepoint: string) => {
    requestAddReaction(reaction_codepoint, chirp.id, laoId).catch((err) => {
      toast.show(`Could not add reaction, error: ${err}`, {
        type: 'danger',
        placement: 'top',
        duration: FOUR_SECONDS,
      });
    });
  };

  // TODO: delete a chirp posted with a PoP token from a previous roll call.
  const isSender =
    currentUserPopTokenPublicKey &&
    currentUserPopTokenPublicKey.valueOf() === chirp.sender.valueOf();

  const deleteDisabled = !isConnected || !currentUserPopTokenPublicKey;

  const deleteChirp = () => {
    if (!currentUserPopTokenPublicKey) {
      return;
    }

    requestDeleteChirp(currentUserPopTokenPublicKey, chirp.id, laoId).catch((err) => {
      toast.show(`Could not remove chirp, error: ${err}`, {
        type: 'danger',
        placement: 'top',
        duration: FOUR_SECONDS,
      });
    });
    setDeleteModalIsVisible(false);
  };

  return (
    <>
      <View style={styles.container}>
        <View style={styles.innerContainer}>
          <View style={styles.leftView}>
            <ProfileIcon publicKey={chirp.sender} />
          </View>
          <View style={styles.rightView}>
            <View style={styles.senderView}>
              <Text style={styles.senderText}>{chirp.sender.valueOf()}</Text>
            </View>
            {chirp.isDeleted ? (
              <Text style={styles.deletedChirpText}>{STRINGS.deleted_chirp}</Text>
            ) : (
              <Text style={styles.chirpText}>{chirp.text}</Text>
            )}
            <View style={styles.reactionsView}>
              {!chirp.isDeleted && (
                <>
                  <View style={styles.reactionView}>
                    <PoPIconButton
                      name="thumbsUp"
                      testID="thumbs-up"
                      onPress={() => addReaction('👍')}
                      disabled={addReactionDisabled}
                    />
                    <Text>{`  ${thumbsUp}`}</Text>
                  </View>
                  <View style={styles.reactionView}>
                    <PoPIconButton
                      name="thumbsDown"
                      testID="thumbs-down"
                      onPress={() => addReaction('👎')}
                      disabled={addReactionDisabled}
                    />
                    <Text>{`  ${thumbsDown}`}</Text>
                  </View>
                  <View style={styles.reactionView}>
                    <PoPIconButton
                      name="heart"
                      testID="heart"
                      onPress={() => addReaction('❤️')}
                      disabled={addReactionDisabled}
                    />
                    <Text>{`  ${heart}`}</Text>
                  </View>
                </>
              )}
            </View>
          </View>
        </View>
        <View style={styles.bottomView}>
          {isSender && !chirp.isDeleted && (
            <View style={styles.deleteChirpContainer}>
              <PoPIconButton
                name="delete"
                testID={`delete_chirp_${chirp.id}`}
                onPress={() => {
                  setDeleteModalIsVisible(true);
                }}
                disabled={deleteDisabled}
              />
            </View>
          )}
          <View style={styles.chirpTimeContainer}>
            <Text style={[Typography.base, Typography.small]}>
              <TimeAgo date={chirp.time.valueOf() * 1000} />
            </Text>
          </View>
        </View>
      </View>
      <ConfirmModal
        visibility={deleteModalIsVisible}
        setVisibility={setDeleteModalIsVisible}
        title={STRINGS.modal_chirp_deletion_title}
        description={STRINGS.modal_chirp_deletion_description}
        onConfirmPress={() => deleteChirp()}
        buttonConfirmText={STRINGS.general_yes}
      />
    </>
  );
};

const propTypes = {
  chirp: PropTypes.instanceOf(Chirp).isRequired,
};

ChirpCard.prototype = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default ChirpCard;
