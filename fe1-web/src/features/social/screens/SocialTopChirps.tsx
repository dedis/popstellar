import * as React from 'react';
import { useContext, useMemo } from 'react';
import { Text, View } from 'react-native';
import { useSelector } from 'react-redux';

import ScreenWrapper from 'core/components/ScreenWrapper';
import { List, Typography } from 'core/styles';
import STRINGS from 'resources/strings';

import { ChirpCard } from '../components';
import { SocialMediaContext } from '../context';
import { SocialHooks } from '../hooks';
import { makeChirpsList, makeReactionsList } from '../reducer';

/**
 * UI for the profile of the current user.
 */

const SocialTopChirps = () => {
  const { currentUserPopTokenPublicKey } = useContext(SocialMediaContext);

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

      const scoreA = (reactionsA['👍'] || 0) + (reactionsA['❤️'] || 0) - (reactionsA['👎'] || 0);
      const scoreB = (reactionsB['👍'] || 0) + (reactionsB['❤️'] || 0) - (reactionsB['👎'] || 0);

      return scoreB - scoreA;
    });
  }, [chirps, reactions]);

  if (!currentUserPopTokenPublicKey) {
    return (
      <View>
        <Text style={Typography.base}>{STRINGS.social_media_your_profile_unavailable}</Text>
      </View>
    );
  }

  return (
    <ScreenWrapper>
      <View>
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
      </View>
    </ScreenWrapper>
  );
};

export default SocialTopChirps;
