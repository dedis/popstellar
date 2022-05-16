import React from 'react';
import { FlatList, StyleSheet, View } from 'react-native';
import { useSelector } from 'react-redux';

import { Spacing } from 'core/styles';
import containerStyles from 'core/styles/stylesheets/containerStyles';

import { selectLaosList } from '../reducer';
import LaoItem from './LaoItem';

const styles = StyleSheet.create({
  flatList: {
    marginTop: Spacing.x2,
  },
});

/**
 * Display a list available of previously connected LAOs
 *
 * TODO use the list that the user have already connect to, and ask data to
 *  some organizer server if needed
 */
const LaoList = () => {
  const laos = useSelector(selectLaosList);
  return (
    <View style={containerStyles.centeredY}>
      <FlatList
        data={laos}
        keyExtractor={(item) => item.id.toString()}
        renderItem={({ item }) => <LaoItem lao={item} />}
        style={styles.flatList}
      />
    </View>
  );
};

export default LaoList;
