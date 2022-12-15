import * as React from 'react';
import { useMemo } from 'react';
import { Text, View } from 'react-native';
import { useSelector } from 'react-redux';

import ScreenWrapper from 'core/components/ScreenWrapper';
import { List, Typography } from 'core/styles';
import STRINGS from 'resources/strings';

import { ChirpCard } from '../components';
import { SocialHooks } from '../hooks';
import { makeChirpsList, makeReactionsList } from '../reducer';

const SocialTopChirps = () => {
  const laoId = SocialHooks.useCurrentLaoId();
  if (laoId === undefined) {
    throw new Error('Impossible to render Social Home, current lao id is undefined');
  }

  const selectChirps = useMemo(() => makeChirpsList(laoId), [laoId]);
  const chirps = useSelector(selectChirps);

  const selectReactions = useMemo(() => makeReactionsList(laoId), [laoId]);
  const reactions = useSelector(selectReactions);

  // sort the chirps in descending order
  const sortedChirpList = useMemo(() => {
    return chirps.sort((a, b) => {
      const reactionsA = reactions[a.id.toState()] || {};
      const reactionsB = reactions[b.id.toState()] || {};

      const scoreA =
        (reactionsA['👍']?.length || 0) +
        (reactionsA['❤️']?.length || 0) -
        (reactionsA['👎']?.length || 0);
      const scoreB =
        (reactionsB['👍']?.length || 0) +
        (reactionsB['❤️']?.length || 0) -
        (reactionsB['👎']?.length || 0);

      return scoreB - scoreA;
    });
  }, [chirps, reactions]);

  if (sortedChirpList.length === 0) {
    return (
      <ScreenWrapper>
        <Text style={[Typography.base, Typography.paragraph]}>
          {STRINGS.social_media_create_chirps_yet}
        </Text>
      </ScreenWrapper>
    );
  }

  return (
    <ScreenWrapper>
      <View style={List.container}>
        {sortedChirpList.map((chirp, i) => (
          <ChirpCard
            key={chirp.id.toString()}
            chirp={chirp}
            isFirstItem={i === 0}
            isLastItem={i === chirps.length - 1}
          />
        ))}
      </View>
    </ScreenWrapper>
  );
};

export default SocialTopChirps;
