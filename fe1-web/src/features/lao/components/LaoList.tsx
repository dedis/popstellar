import React from 'react';
import { View } from 'react-native';
import { useSelector } from 'react-redux';

import { List } from 'core/styles';

import { selectLaosList } from '../reducer';
import LaoItem from './LaoItem';

/**
 * Display a list available of previously connected LAOs
 */
const LaoList = () => {
  const laos = useSelector(selectLaosList);

  return (
    <View style={[List.container, List.top]}>
      {laos.map((lao) => (
        <LaoItem key={lao.id.valueOf()} lao={lao} />
      ))}
    </View>
  );
};

export default LaoList;
