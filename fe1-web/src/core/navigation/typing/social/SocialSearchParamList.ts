import STRINGS from 'resources/strings';

export type SocialSearchParamList = {
  [STRINGS.social_media_search_navigation_attendee_list]: undefined;
  [STRINGS.social_media_search_navigation_user_profile]: {
    userPkString: string;
  };
};
