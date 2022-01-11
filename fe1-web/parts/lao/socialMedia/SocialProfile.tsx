import * as React from 'react';
import PropTypes from 'prop-types';
import { PublicKey } from 'model/objects';
import SocialUserProfile from './SocialUserProfile';

const SocialProfile = (props: IPropTypes) => {
  const { currentUserPublicKey } = props;
  return <SocialUserProfile userPublicKey={currentUserPublicKey} />;
};

const propTypes = {
  currentUserPublicKey: PropTypes.instanceOf(PublicKey).isRequired,
};

SocialProfile.prototype = propTypes;

type IPropTypes = {
  currentUserPublicKey: PublicKey,
};

export default SocialProfile;
