import React, { useState } from 'react';
import { StyleSheet, ViewStyle, View, TextStyle, Text, Pressable } from 'react-native';
import PropTypes from 'prop-types';
import TimeAgo from 'react-timeago';
import { Ionicons } from '@expo/vector-icons';
import { Chirp, PublicKey } from 'model/objects';
import { requestDeleteChirp, requestAddReaction } from 'network';
import STRINGS from 'res/strings';
import { gray } from 'styles/colors';
import { useToast } from 'react-native-toast-notifications';
import { useSelector } from 'react-redux';
import { makeReactionsList } from 'store';
import ProfileIcon from './ProfileIcon';
import ConfirmModal from './ConfirmModal';

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
});

const FOUR_SECONDS = 4000;

const ChirpCard = (props: IPropTypes) => {
  const { chirp } = props;
  const { currentUserPublicKey } = props;
  const toast = useToast();
  const reactions = useSelector(makeReactionsList())[chirp.id.toString()];

  const zero = '  0';
  const thumbsUp = reactions ? reactions['👍'] : 0;
  const thumbsDown = reactions ? reactions['👎'] : 0;
  const heart = reactions ? reactions['❤️'] : 0;

  const [deleteModalIsVisible, setDeleteModalIsVisible] = useState(false);

  const addReaction = (reaction_codepoint: string) => {
    requestAddReaction(reaction_codepoint, chirp.id).catch((err) => {
      toast.show(`Could not add reaction, error: ${err}`, {
        type: 'danger',
        placement: 'top',
        duration: FOUR_SECONDS,
      });
    });
  };

  // TODO: delete a chirp posted with a PoP token from a previous roll call.
  const isSender = currentUserPublicKey.valueOf() === chirp.sender.valueOf();

  const deleteChirp = () => {
    requestDeleteChirp(currentUserPublicKey, chirp.id).catch((err) => {
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
        <View style={{ flexDirection: 'row' }}>
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
                    <Pressable onPress={() => addReaction('👍')} testID="thumbs-up">
                      <Ionicons name="thumbs-up-sharp" size={16} color="black" />
                    </Pressable>
                    <Text>{`  ${thumbsUp}`}</Text>
                  </View>
                  <View style={styles.reactionView}>
                    <Pressable onPress={() => addReaction('👎')} testID="thumbs-down">
                      <Ionicons name="thumbs-down-sharp" size={16} color="black" />
                    </Pressable>
                    <Text>{`  ${thumbsDown}`}</Text>
                  </View>
                  <View style={styles.reactionView}>
                    <Pressable onPress={() => addReaction('❤️')} testID="heart">
                      <Ionicons name="heart" size={16} color="black" />
                    </Pressable>
                    <Text>{`  ${heart}`}</Text>
                  </View>
                </>
              )}
              <View style={styles.reactionView}>
                <Ionicons name="chatbubbles" size={16} color="black" />
                <Text>{zero}</Text>
              </View>
            </View>
          </View>
        </View>
        <View style={styles.bottomView}>
          {isSender && !chirp.isDeleted && (
            <View style={{ marginRight: 'auto' }}>
              <Pressable
                onPress={() => {
                  setDeleteModalIsVisible(true);
                }}
                accessibilityLabel="deleteChirpButton">
                <Ionicons name="close-outline" size={20} color="red" />
              </Pressable>
            </View>
          )}
          <View style={{ marginLeft: 'auto' }}>
            <TimeAgo date={chirp.time.valueOf() * 1000} />
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
        buttonCancelText={STRINGS.general_no}
      />
    </>
  );
};

const propTypes = {
  chirp: PropTypes.instanceOf(Chirp).isRequired,
  currentUserPublicKey: PropTypes.instanceOf(PublicKey).isRequired,
};

ChirpCard.prototype = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default ChirpCard;
