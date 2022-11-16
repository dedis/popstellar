import React from 'react';

import { PublicKey } from 'core/objects';

/**
 * The context that allows access to the current user public key everywhere in the
 * feature.
 */
export const SocialMediaContext = React.createContext<{ currentUserPopTokenPublicKey?: PublicKey }>(
  {
    currentUserPopTokenPublicKey: undefined,
  },
);
