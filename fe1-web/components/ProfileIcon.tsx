import React from 'react';
import PropTypes from 'prop-types';
import Blockies from 'react-blockies';
import { PublicKey } from '../model/objects';
import { popBlue, popGray } from '../styles/colors';

const ProfileIcon = (props: IPropTypes) => {
  const { publicKey } = props;

  return (
    <Blockies
      seed={publicKey.valueOf()}
      size={8}
      scale={5}
      color={popBlue}
      bgColor="#ffffff"
      spotColor={popGray}
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
