import React from 'react';
import { FlatList, StyleSheet, View } from 'react-native';
import { useSelector } from 'react-redux';

import containerStyles from 'core/styles/stylesheets/containerStyles';
import { Spacing } from 'core/styles';

import { Lao } from '../objects';
import { makeLaosList } from '../reducer';
import LaoItem from './LaoItem';

const styles = StyleSheet.create({
  flatList: {
    marginTop: Spacing.s,
  },
});

const useLaoList = (): Lao[] => useSelector(makeLaosList());

/**
 * Display a list available of previously connected LAOs
 *
 * TODO use the list that the user have already connect to, and ask data to
 *  some organizer server if needed
 */
const LaoList = () => {
  const laos = useLaoList();
  return (
    <View style={containerStyles.centered}>
      <FlatList
        data={laos}
        keyExtractor={(item) => item.id.toString()}
        renderItem={({ item }) => <LaoItem LAO={item} />}
        style={styles.flatList}
      />
    </View>
  );
};

export default LaoList;
