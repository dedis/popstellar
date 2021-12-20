import React from 'react';
import PropTypes from 'prop-types';
import Blockies from 'react-blockies';
import { PublicKey } from '../model/objects';

const ProfileIcon = (props: IPropTypes) => {
  const { publicKey } = props;

  return (
    <Blockies
      seed={publicKey.valueOf()}
      size={10}
      scale={4}
      color="#ffffff"
      bgColor="#008ec2"
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
