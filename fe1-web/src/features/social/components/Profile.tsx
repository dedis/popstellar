import PropTypes from 'prop-types';
import React, { useMemo } from 'react';
import { StyleSheet, Text, View, ViewStyle } from 'react-native';
import { useSelector } from 'react-redux';

import { ProfileIcon } from 'core/components';
import { PublicKey } from 'core/objects';
import { Border, Color, List, Spacing, Typography } from 'core/styles';

import { SocialHooks } from '../hooks';
import { makeChirpsListOfUser } from '../reducer';
import ChirpCard from './ChirpCard';

const styles = StyleSheet.create({
  textView: {
    marginTop: Spacing.x1,
  } as ViewStyle,
  userFeed: {
    borderColor: Color.inactive,
    borderTopWidth: Border.width,
    flexDirection: 'column',
    marginTop: Spacing.x1,
  } as ViewStyle,
});

const Profile = ({ publicKey }: IPropTypes) => {
  const laoId = SocialHooks.useCurrentLaoId();
  if (!laoId) {
    throw new Error('Impossible to render Social Profile, current lao id is undefined');
  }

  const userChirps = useMemo(() => makeChirpsListOfUser(laoId)(publicKey), [publicKey, laoId]);
  const userChirpList = useSelector(userChirps);

  return (
    <View>
      <View>
        <ProfileIcon publicKey={publicKey} size={8} scale={10} />
        <View style={styles.textView}>
          <Text style={[Typography.base, Typography.important]} numberOfLines={1}>
            {publicKey.toString()}
          </Text>
          <Text>{`${userChirpList.length} ${
            userChirpList.length === 1 ? 'chirp' : 'chirps'
          }`}</Text>
        </View>
      </View>
      <View style={styles.userFeed}>
        <View style={List.container}>
          {userChirpList.map((chirp, i) => (
            <ChirpCard
              key={chirp.id.toString()}
              chirp={chirp}
              isFirstItem={false /* no round borders at the top */}
              isLastItem={i === userChirpList.length - 1}
            />
          ))}
        </View>
      </View>
    </View>
  );
};

const propTypes = {
  publicKey: PropTypes.instanceOf(PublicKey).isRequired,
};

Profile.prototype = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default Profile;
