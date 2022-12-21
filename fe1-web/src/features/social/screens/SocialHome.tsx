import { CompositeScreenProps, useNavigation } from '@react-navigation/core';
import { StackScreenProps } from '@react-navigation/stack';
import React, { useCallback, useContext, useMemo } from 'react';
import { ListRenderItemInfo, Text, View } from 'react-native';
import { FlatList } from 'react-native-gesture-handler';
import { useSelector } from 'react-redux';

import { PoPIcon } from 'core/components';
import PoPTouchableOpacity from 'core/components/PoPTouchableOpacity';
import ScreenWrapper from 'core/components/ScreenWrapper';
import { AppParamList } from 'core/navigation/typing/AppParamList';
import { LaoParamList } from 'core/navigation/typing/LaoParamList';
import { SocialHomeParamList } from 'core/navigation/typing/SocialHomeParamList';
import { SocialParamList } from 'core/navigation/typing/SocialParamList';
import { Color, Icon, List, Typography } from 'core/styles';
import STRINGS from 'resources/strings';

import { ChirpCard } from '../components';
import { SocialMediaContext } from '../context';
import { SocialHooks } from '../hooks';
import { Chirp } from '../objects';
import { makeChirpsList } from '../reducer';

type NavigationProps = CompositeScreenProps<
  CompositeScreenProps<
    StackScreenProps<SocialHomeParamList, typeof STRINGS.social_media_home_navigation_home>,
    StackScreenProps<SocialParamList, typeof STRINGS.social_media_navigation_tab_home>
  >,
  CompositeScreenProps<
    StackScreenProps<LaoParamList, typeof STRINGS.navigation_social_media>,
    StackScreenProps<AppParamList, typeof STRINGS.navigation_app_lao>
  >
>;

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
      </ScreenWrapper>
    );
  }

  return (
    <ScreenWrapper>
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

export const SocialHomeTopRight = () => {
  const navigation = useNavigation<NavigationProps['navigation']>();

  return (
    <PoPTouchableOpacity
      onPress={() =>
        navigation.navigate(STRINGS.social_media_navigation_tab_home, {
          screen: STRINGS.social_media_home_navigation_new_chirp,
        })
      }
      testID="create_chirp_selector">
      <PoPIcon name="create" color={Color.inactive} size={Icon.size} />
    </PoPTouchableOpacity>
  );
};

export default SocialHome;
