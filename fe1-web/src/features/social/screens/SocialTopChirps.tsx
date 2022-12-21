import * as React from 'react';
import { useMemo } from 'react';
import { Text, View } from 'react-native';
import { useSelector } from 'react-redux';

import ScreenWrapper from 'core/components/ScreenWrapper';
import { List, Typography } from 'core/styles';
import STRINGS from 'resources/strings';

import { ChirpCard } from '../components';
import { SocialHooks } from '../hooks';
import { makeTopChirpsSelector } from '../reducer';

const NUM_TOP_CHIRPS_TO_DISPLAY = 3;

const SocialTopChirps = () => {
  const laoId = SocialHooks.useCurrentLaoId();
  if (laoId === undefined) {
    throw new Error('Impossible to render Social Home, current lao id is undefined');
  }

  const topChirpsSelector = useMemo(
    () => makeTopChirpsSelector(laoId, NUM_TOP_CHIRPS_TO_DISPLAY),
    [laoId],
  );
  const topChirps = useSelector(topChirpsSelector);

  // sort the chirps in descending orde

  if (topChirps.length === 0) {
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
        {topChirps.map((chirp, i) => (
          <ChirpCard
            key={chirp.id.toString()}
            chirp={chirp}
            isFirstItem={i === 0}
            isLastItem={i === topChirps.length - 1}
          />
        ))}
      </View>
    </ScreenWrapper>
  );
};

export default SocialTopChirps;
