import { PublicKey } from 'core/objects';
import STRINGS from 'resources/strings';

export type SocialSearchParamList = {
  [STRINGS.social_media_navigation_tab_attendee_list]: {
    currentUserPublicKey: PublicKey;
  };
  [STRINGS.social_media_navigation_tab_user_profile]: {
    currentUserPublicKey: PublicKey;
    userPublicKey: PublicKey;
  };
};
