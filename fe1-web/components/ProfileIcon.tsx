import React from 'react';
import PropTypes from 'prop-types';
import Blockies from 'react-blockies';
import { PublicKey } from '../model/objects';

const ProfileIcon = (props: IPropTypes) => {
  const { publicKey } = props;

  return (
    <Blockies
      seed={publicKey.valueOf()}
      size={8}
      scale={5}
      color="#008ec2"
      bgColor="#ffffff"
      spotColor="#363636"
    />
  );
};

const propTypes = {
  publicKey: PropTypes.instanceOf(PublicKey).isRequired,
};

ProfileIcon.propTypes = propTypes;

type IPropTypes = {
  publicKey: PublicKey,
};

export default ProfileIcon;
