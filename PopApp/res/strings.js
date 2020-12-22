const STRINGS = {

  /* --- GENERAL strings --- */
  general_button_cancel: 'Cancel',
  general_button_confirm: 'Confirm',
  general_button_open: 'Open',
  general_button_yes: 'Yes',
  general_button_no: 'No',

  /* --- Navigation Strings --- */
  navigation_tab_home: 'Home',
  navigation_tab_connect: 'Connect',
  navigation_tab_launch: 'Launch',

  /* --- App Navigation Strings --- */
  app_navigation_tab_home: 'AppHome',
  app_navigation_tab_organizer: 'AppOrganizer',

  /* --- Organization Navigation Strings --- */
  organization_navigation_tab_home: 'Home',
  organization_navigation_tab_attendee: 'Attendee',
  organization_navigation_tab_identity: 'My indentity',

  /* --- Witness Navigation Strings --- */
  witness_navigation_tab_home: 'Witness',
  witness_navigation_tab_video: 'Witness Video',

  /* --- Organizer Navigation Strings --- */
  organizer_navigation_tab_home: 'Organizer',
  organizer_navigation_tab_create_event: 'Create Event',
  organizer_navigation_tab_add_witness: 'Add witness',
  organizer_navigation_tab_roll_call: 'Roll-Call',

  /* --- Home Strings --- */
  home_welcome: 'Welcome to Personhood.Online!',
  home_connect_lao: 'To connect to a local organization (LAO), please tap to Connect above',
  home_launch_lao: 'To launch a new LAO as on organizer, please tap Launch tab above',

  /* --- Connect Strings --- */
  connect_description: 'The easiest way to connect to a local organization is to scan its QR code',
  connect_button_camera: 'Enable Camera Access',

  // Unapproved Connect Strings
  connect_unapproved_title: 'Unapproved',

  // Scanning Connect Strings
  connect_scanning_title: 'Scanning',
  connect_scanning_camera_view: 'Camera view',

  // Connecting Connect Strings
  connect_connecting_title: 'Connecting',
  connect_connecting_uri: 'Connecting to URI',
  connect_connecting_validate: 'Simulate Validation',

  // Confirm Connect Strings
  connect_confirm_title: 'Confirm',
  connect_confirm_description: 'Connect to this local organization?',

  /* --- Launch Strings --- */
  launch_description: 'To launch a new organization please enter a name for the organization (you can change it later)',
  launch_organization_name: 'Organization name',
  launch_button_launch: 'Launch',

  /* --- Organizer Strings --- */
  organization_name: 'Organization name',

  /* --- Identity Strings --- */
  identity_description: 'Identity screen',
  identity_check_box_anonymous: 'Anonymous',
  identity_check_box_anonymous_description: 'You can participate in organizations and meetings anonymously by leaving '
        + 'this box checked. If you wish to reveal your identity to other participants in the organization, you may '
        + 'un-check this box and enter the information you wish to reaveal below. You must enter identity information in '
        + 'order to play an Organizer or Witness role in an organization.',
  identity_name_placeholder: 'Name',
  identity_title_placeholder: 'Title',
  identity_organization_placeholder: 'Organization',
  identity_email_placeholder: 'Email',
  identity_phone_placeholder: 'Phone number',

  /* --- Witness Strings --- */
  witness_description: 'Witness screen',
  witness_video_button: 'Go to the video screen',
  witness_name: 'Witnesses',
  witness_scan: 'Please scan the personal QR code of the witness to add',

  /* --- Discussion creation Strings --- */
  discussion_create_name: 'Name*',
  discussion_create_open: 'discussion open',

  /* --- Roll-call creation Strings --- */
  roll_call_create_deadline: 'Deadline:',
  roll_call_create_description: 'Description',
  roll_call_create_location: 'Location*',
  roll_call_create_name: 'Name*',

  /* --- Roll-call scanning Strings --- */
  roll_call_scan_description: 'Please scan each participant’s Roll-call QR code exactly once.',
  roll_call_scan_participant: 'participants scanned',
  roll_call_scan_close: 'Close Roll-Call',
  roll_call_scan_confirmation: 'Do you confirm to close the roll-call ?',

  /* --- Poll creation Strings --- */
  poll_create_question: 'Question*',
  poll_create_finish_time: 'Finish time',

  /* --- Meeting creation Strings --- */
  meeting_create_name: 'Name*',
  meeting_create_start_time: 'Start time: ',
  meeting_create_finish_time: 'Finish time: ',
  meeting_create_location: 'Location',

  /* --- General creation Strings --- */
  create_description: 'Choose the type of event you want to create',

  lorem_ipsum: "Scrollable box containing the critical informations!\nLAO's, organizers' and witnesses' names "
        + 'and hex fingerprint\nLorem ipsum dolor sit amet, consectetur adipiscing elit. Suspendisse vitae egestas '
        + 'ex, et rhoncus nibh. Class aptent taciti sociosqu ad litora torquent per conubia nostra, per inceptos '
        + 'himenaeos. Aliquam iaculis elit libero, id lacinia quam vestibulum vitae. Integer tristique non est ac '
        + 'feugiat. Phasellus ac sapien eu ante sodales auctor et id ex. Etiam fringilla pulvinar dui ullamcorper '
        + 'fermentum. Sed luctus lacus vel hendrerit tempus. Vivamus vitae posuere nibh, eleifend semper risus. Mauris '
        + 'sit amet nunc nec risus volutpat semper et a tortor. Donec arcu nisi, pellentesque nec arcu vitae, '
        + 'efficitur molestie tellus. In in felis bibendum orci consectetur sagittis. Phasellus nec faucibus sem. Ut '
        + 'sagittis lorem non tellus luctus, ac lacinia lectus pretium.\nInteger vitae aliquet lorem. Etiam non erat '
        + 'venenatis, venenatis ante et, efficitur ligula. Etiam et pellentesque erat, at fringilla elit. Aliquam '
        + 'facilisis tortor eget metus rhoncus mattis. Sed luctus velit quis enim scelerisque, quis elementum purus '
        + 'cursus. Proin venenatis commodo mi ac sodales. Cras in pretium tellus.\nDuis sollicitudin, urna a tempor '
        + 'dapibus, dui nisl rhoncus dolor, et pretium quam dolor id velit. Donec vitae augue sollicitudin neque '
        + 'ultrices ultrices aliquam vel turpis. Sed quis risus luctus, volutpat libero vel, placerat neque. Nunc '
        + 'luctus malesuada eros, at accumsan lacus vehicula at. Duis laoreet placerat vehicula. Phasellus pulvinar '
        + 'eget orci eget ultrices. Cras in tincidunt libero, eget vulputate mi. Pellentesque hendrerit nibh massa, ac '
        + 'tincidunt lorem interdum a. Etiam a sodales justo. Ut ut ipsum eget lacus finibus tristique quis sit amet '
        + 'turpis. Nulla suscipit, nunc ut accumsan laoreet, felis tellus venenatis magna, a malesuada tortor risus et '
        + 'odio. Nulla vehicula libero ut elit lacinia pretium.\nNunc consectetur pharetra tortor, ut elementum quam '
        + 'dapibus a. Vestibulum vel tincidunt felis. Duis dapibus elit eu suscipit sles. Integer nec ultricies orci, '
        + 'at porta odio. Etiam sed sem condimentum, feugiat ex nec, bibendum nulla. Donec venenatis magna vel odio '
        + 'molestie porttitor. Donec maximus placerat auctor. Fusce scelerisque condimentum molestie. Duis a lorem '
        + 'pretium, imperdiet massa a, iaculis dolor. Nullam a nisl elementum sapien facilisis scelerisque quis in '
        + 'sapien. Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.\nInteger sit '
        + 'amet quam vel turpis ultricies tristique ac at mauris. Vestibulum efficitur fringilla lacus non fringilla. '
        + 'Quisque venenatis dui tempor, aliquam nisi ut, cursus ante. Vestibulum ante ipsum primis in faucibus orci '
        + 'luctus et ultrices posuere cubilia curae; Vestibulum facilisis sem congue sem semper consectetur. Nunc a '
        + 'scelerisque diam, vulputate lobortis erat. Aenean posuere faucibus consectetur. Praesent feugiat nulla '
        + 'porta orci auctor, a vulputate felis suscipit. Aenean vulputate ligula ac commodo ornare.',

  unused: 'unused',
};

export default STRINGS;
