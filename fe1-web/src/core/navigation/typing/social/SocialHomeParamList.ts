import STRINGS from 'resources/strings';

export type SocialHomeParamList = {
  [STRINGS.social_media_home_navigation_home]: undefined;
  [STRINGS.social_media_home_navigation_new_chirp]: undefined;
  [STRINGS.social_media_navigation_user_profile]: {
    userPkString: string;
  };
};
