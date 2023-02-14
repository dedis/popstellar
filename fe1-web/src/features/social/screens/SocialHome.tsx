import React, { useCallback, useContext, useMemo } from 'react';
import { ListRenderItemInfo, Text, View } from 'react-native';
import { FlatList } from 'react-native-gesture-handler';
import { useSelector } from 'react-redux';

import ScreenWrapper from 'core/components/ScreenWrapper';
import { List, Typography } from 'core/styles';
import STRINGS from 'resources/strings';

import { ChirpCard } from '../components';
import NewChirp from '../components/NewChirp';
import { SocialMediaContext } from '../context';
import { SocialHooks } from '../hooks';
import { Chirp } from '../objects';
import { makeChirpsList } from '../reducer';

const SocialHome = () => {
  const laoId = SocialHooks.useCurrentLaoId();
  const { currentUserPopTokenPublicKey } = useContext(SocialMediaContext);

  if (laoId === undefined) {
    throw new Error('Impossible to render Social Home, current lao id is undefined');
  }

  const chirps = useMemo(() => makeChirpsList(laoId), [laoId]);
  const chirpList = useSelector(chirps);

  const renderChirp = useCallback(
    ({ item: chirp, index: i }: ListRenderItemInfo<Chirp>) => (
      <ChirpCard chirp={chirp} isFirstItem={i === 0} isLastItem={i === chirpList.length - 1} />
    ),
    [chirpList],
  );

  if (chirpList.length === 0) {
    return (
      <ScreenWrapper>
        <Text style={[Typography.base, Typography.paragraph]}>
          {STRINGS.social_media_create_chirps_yet}
        </Text>
        {currentUserPopTokenPublicKey ? (
          <Text style={[Typography.base, Typography.paragraph]}>
            {STRINGS.social_media_howto_create_chirps}
          </Text>
        ) : (
          <Text style={[Typography.base, Typography.paragraph]}>
            {STRINGS.social_media_create_chirp_no_pop_token}
          </Text>
        )}
        <NewChirp />
      </ScreenWrapper>
    );
  }

  return (
    <ScreenWrapper>
      <NewChirp />
      <View style={List.container}>
        <FlatList
          data={chirpList}
          renderItem={renderChirp}
          keyExtractor={(item) => item.id.toString()}
        />
      </View>
    </ScreenWrapper>
  );
};

export default SocialHome;
