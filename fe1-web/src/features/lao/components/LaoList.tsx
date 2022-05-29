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
    <View style={List.container}>
      {laos.map((lao, idx) => (
        <LaoItem
          key={lao.id.valueOf()}
          lao={lao}
          isFirstItem={idx === 0}
          isLastItem={idx === laos.length - 1}
        />
      ))}
    </View>
  );
};

export default LaoList;
