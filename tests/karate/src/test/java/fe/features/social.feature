Feature: Social Media
  Background:
    * call read('classpath:fe/utils/constants.feature')
    * call read(MOCK_CLIENT_FEATURE)
    * def organizer = createMockClient()
    * def lao = organizer.createLao()

  @name=social_open_without_pop_token
  Scenario: Open social media page without pop token
    # Delay are added to give time for the menu to open/close
    * call read(PLATFORM_FEATURE) { name: '#(JOIN_LAO)', params: { lao: '#(lao)' } }
    Given waitFor(drawer_menu_button).click()
    And delay(500)
    When click(drawer_menu_social)
    Then waitFor(social_home_page)
    And delay(500)
    And screenshot()

  @name=social_send_message
  Scenario: Send chirp message
    Given call read(PLATFORM_FEATURE) { name: '#(JOIN_ROLLCALL)', params: { lao: '#(lao)', organizer: '#(organizer)' } }
    And call read(PLATFORM_FEATURE) { name: '#(SWITCH_TO_SOCIAL_PAGE)' }
    And def message = 'Hello from the test'
    And waitFor(social_chirp_input).input(message)
    And screenshot()
    And click(social_chirp_publish_button)
    Then waitForText(social_chirp_message, message)
    And screenshot()

  @name=social_receive_message
  Scenario: Receive chirp message
    Given call read(PLATFORM_FEATURE) { name: '#(JOIN_ROLLCALL)', params: { lao: '#(lao)', organizer: '#(organizer)' } }
    And call read(PLATFORM_FEATURE) { name: '#(SWITCH_TO_SOCIAL_PAGE)' }
    And def message = 'Hello from the test'
    When organizer.sendChirp(lao, message)
    Then waitForText(social_chirp_message, message)
    And screenshot()

  @name=social_react_message
  Scenario: React to chirp message
    Given call read(PLATFORM_FEATURE) { name: '#(JOIN_ROLLCALL)', params: { lao: '#(lao)', organizer: '#(organizer)' } }
    And call read(PLATFORM_FEATURE) { name: '#(SWITCH_TO_SOCIAL_PAGE)' }
    And def message = 'Hello from the test'
    When organizer.sendChirp(lao, message)
    And waitFor(social_chirp_like_button).click()
    And waitFor(social_chirp_dislike_button).click()
    And waitFor(social_chirp_love_button).click()
    Then waitForText(social_chirp_like_count, '1')
    And waitForText(social_chirp_dislike_count, '1')
    And waitForText(social_chirp_love_count, '1')
    And screenshot()

  @name=social_top_chirps_page
  Scenario: Open top chirps page
    Given call read(PLATFORM_FEATURE) { name: '#(JOIN_ROLLCALL)', params: { lao: '#(lao)', organizer: '#(organizer)' } }
    And call read(PLATFORM_FEATURE) { name: '#(SWITCH_TO_SOCIAL_PAGE)' }
    And def message = 'Hello from the test'
    And organizer.sendChirp(lao, message)
    When waitFor(social_menu_top_chirps_button).click()
    Then waitFor(social_top_chirps_page)
    And waitForText(social_chirp_message, message)
    And screenshot()
