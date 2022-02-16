import * as React from 'react';
import {
  FlatList, ListRenderItemInfo, StyleSheet, View, ViewStyle,
} from 'react-native';
import { useSelector } from 'react-redux';
import PropTypes from 'prop-types';

import STRINGS from 'res/strings';
import { makeCurrentLao } from 'store';
import { PublicKey } from 'model/objects';
import TextBlock from 'components/TextBlock';
import { gray } from 'styles/colors';
import { makeLastRollCallAttendeesList } from 'features/events/reducer/EventsReducer';

import UserListItem from '../components/UserListItem';

/**
 * Component that will be used to allow users to search for other users or topics.
 * For now, it is used to show all the attendees of the last roll call so that everyone can follow
 * whoever they want.
 */
const styles = StyleSheet.create({
  viewCenter: {
    alignSelf: 'center',
    width: 600,
  } as ViewStyle,
  titleTextView: {
    alignSelf: 'flex-start',
    marginTop: 20,
  } as ViewStyle,
  attendeesList: {
    flexDirection: 'column',
    marginTop: 20,
    borderTopWidth: 1,
    borderColor: gray,
  } as ViewStyle,
});

const SocialSearch = (props: IPropTypes) => {
  const { currentUserPublicKey } = props;
  const laoSelect = makeCurrentLao();
  const currentLao = useSelector(laoSelect);

  if (!currentLao) {
    throw new Error('Impossible to open Social media Search if you are not connected to a LAO');
  }

  const rollCallId = currentLao.last_tokenized_roll_call_id;
  const attendeesSelect = makeLastRollCallAttendeesList(currentLao.id, rollCallId);
  const attendees = useSelector(attendeesSelect);

  const renderItem = ({ item }: ListRenderItemInfo<PublicKey>) => {
    // Not show our own profile
    if (item.valueOf() === currentUserPublicKey.valueOf()) {
      return null;
    }
    return (
      <UserListItem
        laoId={currentLao.id}
        publicKey={item}
        currentUserPublicKey={currentUserPublicKey}
      />
    );
  };

  return (
    <View style={styles.viewCenter}>
      <View style={styles.titleTextView}>
        <TextBlock text={STRINGS.attendees_of_last_roll_call} />
      </View>
      <View style={styles.attendeesList}>
        <FlatList
          data={attendees}
          renderItem={renderItem}
          keyExtractor={(publicKey) => publicKey.valueOf()}
        />
      </View>
    </View>
  );
};

const propTypes = {
  currentUserPublicKey: PropTypes.instanceOf(PublicKey).isRequired,
};

SocialSearch.prototype = propTypes;

type IPropTypes = {
  currentUserPublicKey: PublicKey,
};

export default SocialSearch;
