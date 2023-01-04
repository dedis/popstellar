import STRINGS from 'resources/strings';

export type SocialProfileParamList = {
  [STRINGS.social_media_profile_navigation_profile]: undefined;
  [STRINGS.social_media_navigation_user_profile]: {
    userPkString: string;
  };
};
