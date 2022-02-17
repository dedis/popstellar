import React from 'react';
import PropTypes from 'prop-types';
import Blockies from 'react-blockies';
import { popBlue } from 'styles/colors';
import { PublicKey } from 'model/objects';

const ProfileIcon = (props: IPropTypes) => {
  const { publicKey, size, scale } = props;

  return (
    <Blockies
      seed={publicKey.valueOf()}
      size={size}
      scale={scale}
      spotColor={popBlue}
      bgColor="#ffffff"
    />
  );
};

const propTypes = {
  publicKey: PropTypes.instanceOf(PublicKey).isRequired,
  size: PropTypes.number,
  scale: PropTypes.number,
};

ProfileIcon.defaultProps = {
  size: 8,
  scale: 5,
};

ProfileIcon.propTypes = propTypes;

type IPropTypes = {
  publicKey: PublicKey,
  size: number,
  scale: number,
};

export default ProfileIcon;
