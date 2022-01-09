import * as React from 'react';
import {
  FlatList, ListRenderItemInfo, StyleSheet, View, ViewStyle,
} from 'react-native';
import STRINGS from 'res/strings';
import { makeCurrentLao, makeLastRollCallAttendeesList } from 'store';
import { useSelector } from 'react-redux';
import { PublicKey } from 'model/objects';
import TextBlock from 'components/TextBlock';
import UserListItem from 'components/UserListItem';
import { gray } from 'styles/colors';

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

const SocialSearch = () => {
  const laoSelect = makeCurrentLao();
  const currentLao = useSelector(laoSelect);

  if (!currentLao) {
    throw new Error('Impossible to open Social media Search if you are not connected to a LAO');
  }

  const rollCallId = currentLao.last_tokenized_roll_call_id;
  const attendeesSelect = makeLastRollCallAttendeesList(currentLao.id, rollCallId);
  const attendees = useSelector(attendeesSelect);

  const renderItem = ({ item }: ListRenderItemInfo<PublicKey>) => (
    <UserListItem laoId={currentLao.id} publicKey={item} />
  );

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

export default SocialSearch;
