/* eslint-disable @typescript-eslint/naming-convention */

namespace STRINGS {
  /* --- GENERAL strings --- */
  export const general_button_cancel = 'Cancel';
  export const general_button_confirm = 'Confirm';
  export const general_button_open = 'Open';
  export const general_button_ok = 'Ok';
  export const general_yes = 'Yes';
  export const general_no = 'No';
  export const general_add = 'Add';
  export const general_done = 'Done';
  export const general_starting_at = 'Supposed to start';
  export const general_ending_at = 'Supposed to end';
  export const general_ended_at = 'Ended at';

  export const general_closed = 'Closed';
  export const general_ongoing = 'Ongoing';

  /* --- User Roles --- */
  export const user_role = 'Role';
  export const user_role_attendee = 'Attendee';
  export const user_role_organizer = 'Organizer';
  export const user_role_witness = 'Witness';

  export const navigation_social_media = 'Social Media';

  /* --- App Navigation Strings --- */
  export const navigation_app_home = 'AppHome';
  export const navigation_app_lao = 'AppLao';
  export const navigation_app_wallet_create_seed = 'Wallet Seed Creation';
  export const navigation_app_wallet_insert_seed = 'Wallet Recovery';
  export const navigation_app_connect = 'Connect';

  /* --- HomeNavigation Strings --- */
  export const navigation_home_home = 'Home';
  export const navigation_home_mock_connect = 'MockConnect';
  export const navigation_home_wallet = 'Wallet';

  /* --- ConnectionNavigation Strings --- */
  export const navigation_connect_scan = 'Scanning';
  export const navigation_connect_launch = 'Launch';
  export const navigation_connect_confirm = 'Confirm';

  /* --- Notification Navigation Strings --- */
  export const navigation_notification_notifications = 'NotificationNavigation Notifications';
  export const navigation_notification_notifications_title = 'Notifications';
  export const navigation_notification_single_notification = 'Notification';

  /* --- Lao Navigation Strings --- */
  export const navigation_lao_home = 'Lao';
  export const navigation_lao_lao_title = 'LAO';
  export const navigation_lao_notifications = 'Notifications';
  export const navigation_lao_events = 'Events';
  export const navigation_lao_wallet = 'Wallet';

  /* --- Lao Home Navigation Strings --- */
  export const navigation_lao_home_identity = 'My identity';

  /* --- Lao Organizer Navigation Strings --- */
  export const navigation_lao_events_home = 'Events Home';
  export const navigation_lao_events_home_title = 'Events';

  export const navigation_lao_events_create_event = 'Create Event';
  export const navigation_lao_events_create_meeting = 'Create Meeting';
  export const navigation_lao_events_view_single_meeting = 'Single Meeting';
  export const navigation_lao_events_create_roll_call = 'Create Roll-Call';
  export const navigation_lao_events_view_single_roll_call = 'Single Roll-Call';
  export const navigation_lao_events_create_election = 'Create Election';
  export const navigation_lao_events_view_single_election = 'Single Election';
  export const navigation_lao_events_open_roll_call = 'Open Roll-Call';

  /* --- Wallet Navigation Strings --- */
  export const navigation_wallet_home_tab = 'Wallet Home';
  export const navigation_wallet_setup_tab = 'Wallet Setup';
  export const navigation_wallet_show_seed = 'New Wallet';
  export const navigation_wallet_synced = 'My Wallet';

  /* --- Social Media Navigation Strings --- */
  export const social_media_navigation_tab_home = 'Home';
  export const social_media_navigation_tab_search = 'Search';
  export const social_media_navigation_tab_follows = 'My Follows';
  export const social_media_navigation_tab_profile = 'My Profile';
  export const social_media_navigation_tab_user_profile = 'User profile';
  export const social_media_navigation_tab_attendee_list = 'List of attendees';

  /* --- Lao Strings --- */
  export const lao_qr_code_title = 'Connection QR Code';
  export const lao_properties_id = 'Identifier';
  export const lao_properties_your_role = 'Your role';
  export const lao_properties_current_connections = 'You are currently connected to';
  export const lao_properties_add_additional_connection = 'Add connection';
  export const lao_properties_disconnect = 'Disconnect';

  /* --- Home Strings --- */

  export const home_navigation_title = 'LAOs';
  export const home_setup_heading = 'POPStellar 🤩';
  export const home_setup_description_1 =
    'The POPStellar application builds on top of so-called local autonomous organizations (LAOs). ' +
    'Known LAOs will be listed here after you connected to it once.';
  export const home_setup_description_2 =
    'You can connect to a LAO by tapping "Connect" in the bottom navigation bar and then scanning the qr code a LAO organizer provides to you.';

  /* --- Social Media Strings --- */
  export const button_publish = 'Publish';
  export const your_chirp = 'Your chirp';
  export const deleted_chirp = 'This chirp was deleted';
  export const attendees_of_last_roll_call = 'Attendees of last roll-call';
  export const follow_button = 'Follow';
  export const profile_button = 'Profile';
  export const social_media_your_profile_unavailable =
    'You do not have a social media profile yet, be sure to have participated in a roll-call.';
  export const modal_chirp_deletion_title = 'Chirp deletion';
  export const modal_chirp_deletion_description = 'Are you sure you want to delete this chirp?';

  /* --- Connect Strings --- */
  export const connect_description =
    'The easiest way to connect to a local organization is to scan its QR code';
  export const connect_scanning_fail = 'Invalid QRCode data';

  // Connecting Connect Strings
  export const connect_connect = 'Connect';
  export const connect_connecting_title = 'Connecting';
  export const connect_connecting_uri = 'Connecting to URI';
  export const connect_connecting_validate = 'Simulate connection';
  export const connect_server_uri = 'Server URI';
  export const connect_server_uri_placeholder = 'ws://127.0.0.1:9000/';
  export const connect_lao_id = 'LAO ID';
  export const connect_lao_id_placeholder = 'QVGlyoTAEYWxq3hSBuawE-lo3sHEyfIv8uizjQTzsIU=';

  // Confirm Connect Strings
  export const connect_confirm_description = 'Connect to this local organization?';

  /* --- Launch Strings --- */
  export const launch_heading = 'Launch a new organization 🚀';
  export const launch_organization_name = 'Organization name';
  export const launch_address = 'Address';
  export const launch_button_launch = 'Launch';

  /* --- OrganizerScreen Strings --- */
  export const organization_name = 'Organization name';

  /* --- Identity Strings --- */
  export const identity_description = 'Identity screen';
  export const identity_check_box_anonymous = 'Anonymous';
  export const identity_check_box_anonymous_description =
    'You can participate in organizations and meetings anonymously by leaving ' +
    'this box checked. If you wish to reveal your identity to other participants in the organization, you may ' +
    'un-check this box and enter the information you wish to reaveal below. You must enter identity information in ' +
    'order to play an Organizer or Witness role in an organization.';

  export const identity_name_label = 'Name';
  export const identity_name_placeholder = 'John Doe';

  export const identity_title_label = 'Title';
  export const identity_title_placeholder = 'LAO Organizer';

  export const identity_organization_label = 'Organization';
  export const identity_organization_placeholder = 'DEDIS Lab';

  export const identity_email_label = 'Email';
  export const identity_email_placeholder = 'john.doe@epfl.ch';

  export const identity_phone_label = 'Phone number';
  export const identity_phone_placeholder = '+41 12 345 67 89';

  export const identity_qrcode_description = 'ID (Public Key):';

  /* --- WitnessScreen Strings --- */
  export const witness_description = 'Witness screen';
  export const witness_video_button = 'Go to the video screen';
  export const witness_name = 'Witnesses';
  export const witness_scan = 'Please scan the personal QR code of the witness to add';

  /* --- Evoting Feature Strings --- */
  export const election_event_name = 'Election';

  /* --- Election creation Strings --- */
  export const election_create_setup = 'Election Setup';
  export const election_create_name = 'Name';
  export const election_create_name_placeholder = 'Election Q1 2022';
  export const election_create_version = 'Election Type';
  export const election_create_version_open_ballot = 'Open Ballot';
  export const election_create_version_secret_ballot = 'Secret Ballot';
  export const election_create_start_time = 'Start time';
  export const election_create_finish_time = 'End time';
  export const election_create_question = 'Question';
  export const election_create_question_placeholder = 'What is your favorite color?';
  export const election_create_option_placeholder = 'Blue';

  export const election_create_add_question = 'Add Question';

  export const election_create_ballot_options = 'Ballot Options';

  export const election_create_voting_method = 'Voting method';
  export const election_create_ballot_option = 'Option';
  export const election_voting_method = 'Voting Method';
  export const election_method_Plurality = 'Plurality';
  export const election_method_Approval = 'Approval';
  export const election_wait_for_election_key = 'Waiting for the election key to be broadcasted';

  export const election_open = 'Open Election';
  export const election_add_question = 'Add Question';
  export const election_end = 'End Election and Tally Votes';
  export const election_results = 'Election Results';
  export const election_warning_open_ballot =
    'This is an open ballot election, your cast votes will be visible to all members of the LAO.';
  export const election_info_secret_ballot =
    'This is a secret ballot election, your cast votes are encrypted and will be decrypted by the organizer of the LAO.';

  /* --- Event Creation Strings --- */
  export const modal_event_creation_failed = 'Event creation failed';
  export const modal_event_ends_in_past = "The event's end time is in the past.";
  export const modal_event_starts_in_past =
    "The event's start time is in the past.\nWhat do you want to do ?";
  export const modal_button_start_now = 'Start it now';
  export const modal_button_go_back = 'Cancel';

  /* --- Cast Vote Strings --- */
  export const cast_vote = 'Cast Vote';

  /* --- Roll-Call Feature Strings --- */
  export const roll_call_event_name = 'Roll-Call';

  export const roll_call_description = 'Description';
  export const roll_call_location = 'Location';
  export const roll_call_open_organizer =
    'The Roll Call is currently open and you as the organizer should start adding attendees by scanning their PoP tokens.';
  export const roll_call_open_attendee =
    'The Roll Call is currently open and you as an attendee should let the organizer scan your PoP token encoded in the QR Code below.';

  export const roll_call_location_error_open_roll_call = 'Unable to send roll call open request';
  export const roll_call_location_error_scanning_no_alias =
    'Unable to scan attendees, the event does not have an idAlias';
  export const roll_call_location_error_reopen_roll_call =
    'Unable to send Roll call re-open request';
  export const roll_call_location_error_reopen_roll_call_no_alias =
    'Unable to send roll call re-open request, the event does not have an idAlias';

  export const roll_call_location_error_close_roll_call = 'Could not close roll call';

  /* --- Roll-call creation Strings --- */

  export const roll_call_create_proposed_start = 'Proposed Start';
  export const roll_call_create_proposed_end = 'Proposed End';
  export const roll_call_create_name = 'Name';
  export const roll_call_create_name_placeholder = 'Real Life Captcha';

  export const roll_call_create_location_placeholder = 'BC410';

  export const roll_call_create_description_placeholder = 'Are you a robot?';

  /* --- Roll-call Single View Strings --- */

  export const roll_call_open = 'Open Roll-Call';
  export const roll_call_reopen = 'Re-open Roll-Call';
  export const roll_call_close = 'Close Roll-Call';

  /* --- Roll-call scanning Strings --- */
  export const roll_call_scan_attendees = 'Scan Attendees';
  export const roll_call_scan_description =
    'Please scan each participant’s Roll-call QR code once.';
  export const roll_call_scan_participant = 'participant scanned';
  export const roll_call_scan_close = 'Close Roll-Call';
  export const roll_call_scan_close_confirmation = 'Do you confirm to close the roll-call ?';

  /* --- Roll-call manually add attendee manually Strings --- */
  export const roll_call_add_attendee_manually = 'Add an attendee manually';
  export const roll_call_modal_add_attendee = 'Add an attendee';
  export const roll_call_modal_enter_token = 'Enter token:';
  export const roll_call_participant_added = 'participant added';
  export const roll_call_invalid_token = 'invalid participant token';
  export const roll_call_attendee_token_placeholder = 'Attendee token';

  /* --- Poll creation Strings --- */
  export const poll_create_question = 'Question*';
  export const poll_create_finish_time = 'Finish time';
  export const poll_create_answer_type_any_of_n = 'Approve any of n';
  export const poll_create_answer_type_one_of_n = 'Choose one of n';

  /* --- Meeting creation Strings --- */
  export const meeting_create_name = 'Name';
  export const meeting_create_name_placeholder = 'Global PoP Meeting';
  export const meeting_create_start_time = 'Start time';
  export const meeting_create_finish_time = 'End time';
  export const meeting_create_location = 'Location';
  export const meeting_create_location_placeholder = 'BC410';

  /* --- Events Feature Strings --- */
  export const events_create_event = 'Create';
  export const events_list_past = 'Past';
  export const events_list_current = 'Current';
  export const events_list_upcoming = 'Upcoming';

  /* --- Notification screen Strings --- */
  export const notification_clear_all = 'Clear notifications';

  /* --- Time Display Strings --- */
  export const time_display_start = 'Start: ';
  export const time_display_end = 'End: ';

  /* --- Wallet Strings --- */

  export const wallet_set_seed_error = 'A synchronization error with your wallet occurred';

  /* --- Wallet Welcome Screen Strings --- */
  export const wallet_welcome_heading = 'Hey there 👋';
  export const wallet_welcome_text_first_time =
    'It seems as if you are using POPStellar for the first time?';

  export const wallet_welcome_text_wallet_explanation_1 =
    'As a first step, you need to set up your';
  export const wallet_welcome_text_wallet_explanation_2 = 'that is secured using a';
  export const wallet_welcome_text_wallet_explanation_wallet = 'wallet 🔒';
  export const wallet_welcome_text_wallet_explanation_seed = 'seed 🔑';
  export const wallet_welcome_text_wallet_explanation_3 =
    'It is very important that you keep a backup copy of your';
  export const wallet_welcome_text_wallet_explanation_4 =
    ', as it is the only way to restore your PoP tokens.';

  export const wallet_welcome_text_wallet_explanation_5 = 'If you have backed up the above';

  export const wallet_welcome_text_wallet_explanation_6 =
    ', you can continue and start using the POPStellar application. ' +
    'The purpose of this application is to demonstrate use cases for proofs of personhood (PoP).';

  export const wallet_welcome_start_exploring = 'Start exploring POPStellar 🤩';

  export const wallet_welcome_already_know_seed =
    'I have already used this application and know previous seed 🧐';

  /* --- Wallet Restore Seed Screen Strings --- */
  export const wallet_restore_heading = 'My 12 word seed 🔑 is ...';
  export const wallet_restore_using_known_seed = 'Restore my wallet 🔐';
  export const wallet_previous_seed_not_known = 'Oof, I might not know my seed 🔑 after all 😬';
  export const wallet_restore_seed_example =
    'grape sock height they tiny voyage kid young domain trumpet three patrol';

  /* --- Wallet Home Screen Strings --- */
  export const wallet_home_header = 'Your Wallet 🔒';
  export const wallet_home_logout = 'Logout';
  export const wallet_home_toggle_debug = 'Toggle debug mode';

  export const no_tokens_in_wallet =
    'No token is yet associated with your wallet seed, once you participate in a roll-call event your PoP tokens will be shown here';

  /* --- Meeting Feature Strings --- */
  export const meeting_event_name = 'Meeting';
  export const meeting_create_meeting = 'Create meeting';

  /* --- Witness Feature Strings --- */
  export const witness_message_witness = 'Witness Message';
  export const meeting_message_decline = 'Decline Message';

  /* --- General creation Strings --- */
  export const create_description = 'Choose the type of event you want to create';
  export const add_option = 'Add option';

  export const lorem_ipsum =
    "Scrollable box containing the critical informations!\nLAO's, organizers' and witnesses' names " +
    'and hex fingerprint\nLorem ipsum dolor sit amet, consectetur adipiscing elit. Suspendisse vitae egestas ' +
    'ex, et rhoncus nibh. Class aptent taciti sociosqu ad litora torquent per conubia nostra, per inceptos ' +
    'himenaeos. Aliquam iaculis elit libero, id lacinia quam vestibulum vitae. Integer tristique non est ac ' +
    'feugiat. Phasellus ac sapien eu ante sodales auctor et id ex. Etiam fringilla pulvinar dui ullamcorper ' +
    'fermentum. Sed luctus lacus vel hendrerit tempus. Vivamus vitae posuere nibh, eleifend semper risus. Mauris ' +
    'sit amet nunc nec risus volutpat semper et a tortor. Donec arcu nisi, pellentesque nec arcu vitae, ' +
    'efficitur molestie tellus. In in felis bibendum orci consectetur sagittis. Phasellus nec faucibus sem. Ut ' +
    'sagittis lorem non tellus luctus, ac lacinia lectus pretium.\nInteger vitae aliquet lorem. Etiam non erat ' +
    'venenatis, venenatis ante et, efficitur ligula. Etiam et pellentesque erat, at fringilla elit. Aliquam ' +
    'facilisis tortor eget metus rhoncus mattis. Sed luctus velit quis enim scelerisque, quis elementum purus ' +
    'cursus. Proin venenatis commodo mi ac sodales. Cras in pretium tellus.\nDuis sollicitudin, urna a tempor ' +
    'dapibus, dui nisl rhoncus dolor, et pretium quam dolor id velit. Donec vitae augue sollicitudin neque ' +
    'ultrices ultrices aliquam vel turpis. Sed quis risus luctus, volutpat libero vel, placerat neque. Nunc ' +
    'luctus malesuada eros, at accumsan lacus vehicula at. Duis laoreet placerat vehicula. Phasellus pulvinar ' +
    'eget orci eget ultrices. Cras in tincidunt libero, eget vulputate mi. Pellentesque hendrerit nibh massa, ac ' +
    'tincidunt lorem interdum a. Etiam a sodales justo. Ut ut ipsum eget lacus finibus tristique quis sit amet ' +
    'turpis. Nulla suscipit, nunc ut accumsan laoreet, felis tellus venenatis magna, a malesuada tortor risus et ' +
    'odio. Nulla vehicula libero ut elit lacinia pretium.\nNunc consectetur pharetra tortor, ut elementum quam ' +
    'dapibus a. Vestibulum vel tincidunt felis. Duis dapibus elit eu suscipit sles. Integer nec ultricies orci, ' +
    'at porta odio. Etiam sed sem condimentum, feugiat ex nec, bibendum nulla. Donec venenatis magna vel odio ' +
    'molestie porttitor. Donec maximus placerat auctor. Fusce scelerisque condimentum molestie. Duis a lorem ' +
    'pretium, imperdiet massa a, iaculis dolor. Nullam a nisl elementum sapien facilisis scelerisque quis in ' +
    'sapien. Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.\nInteger sit ' +
    'amet quam vel turpis ultricies tristique ac at mauris. Vestibulum efficitur fringilla lacus non fringilla. ' +
    'Quisque venenatis dui tempor, aliquam nisi ut, cursus ante. Vestibulum ante ipsum primis in faucibus orci ' +
    'luctus et ultrices posuere cubilia curae; Vestibulum facilisis sem congue sem semper consectetur. Nunc a ' +
    'scelerisque diam, vulputate lobortis erat. Aenean posuere faucibus consectetur. Praesent feugiat nulla ' +
    'porta orci auctor, a vulputate felis suscipit. Aenean vulputate ligula ac commodo ornare.';

  export const unused = 'unused';
}

export default STRINGS;
