@ignore @report=false
Feature: web page object
  Background:
    # Functions
    * def actionSheetClick = (text) => script("setTimeout(() => document.evaluate('//div[text()=\\'" + text + "\\']', document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue.click(), 1000)")

    # Wallet screen
    * def wallet_seed_wallet_text = "[data-testid='seed_wallet_text']"
    * def wallet_new_wallet_button = "[data-testid='exploring_selector']"
    * def wallet_goto_restore_wallet_button = "{}Restore"
    * def wallet_restore_wallet_button = "{}Restore Wallet"
    * def wallet_restore_input = "input"

    # Lao screen
    * def lao_create_button = "{}Create"
    * def lao_join_button = "{}Join"
    * def lao_organization_name_input = "input[data-testid='launch_organization_name_selector']"
    * def lao_server_url_input = "input[data-testid='launch_address_selector']"
    * def lao_launch_button = "[data-testid='launch_launch_selector']"
    * def lao_enter_manually_button = "{}Enter Manually"
    * def lao_enter_manually_server_input = "input[placeholder='Server URI']"
    * def lao_enter_manually_lao_input = "input[placeholder='LAO ID']"
    * def lao_enter_manually_submit_button = "[data-testid='connect-button']"
    * def lao_name = "[data-testid='lao_name_text']"

    # Event screen
    * def event_create_button = "[data-testid='create_event_selector']"
    * def event_create_rollcall = "Create Roll-Call"
    * def event_title = "{}Events"
    * def event_rollcall_name_input = "input[data-testid='roll_call_name_selector']"
    * def event_rollcall_location_input = "input[data-testid='roll_call_location_selector']"
    * def event_rollcall_confirm_button = "[data-testid='roll_call_confirm_selector']"
    * def event_rollcall_pop_token = "div[data-testid='roll_call_pop_token']"
    * def event_rollcall_first_attendee = "div[data-testid='attendee_0']"
    * def event_first_current_event = "[data-testid='current_event_selector_0']"
    * def event_open_rollcall_button = "{}Open Roll-Call"
    * def event_close_rollcall_button = "{}Close Roll-Call"

    # Drawer menu
    * def drawer_menu_button = "[data-testid='drawer_menu_button']"
    * def drawer_menu_container = "[data-testid='drawer_menu_container']"
    * def drawer_menu_social = "[data-testid='drawer_menu_social_media']"
    * def drawer_menu_disconnect = "[data-testid='drawer_menu_disconnect_button']"
    * def drawer_menu_digital_cash = "[data-testid='drawer_menu_digital_cash']"

    # Social screen
    * def social_home_page = "[data-testid='social_home_page']"
    * def social_menu_home_button = "[data-testid='social_menu_home_button']"
    * def social_profile_page = "[data-testid='social_profile_page']"
    * def social_menu_profile_button = "[data-testid='social_menu_profile_button']"
    * def social_search_page = "[data-testid='social_search_page']"
    * def social_menu_search_button = "[data-testid='social_menu_search_button']"
    * def social_top_chirps_page = "[data-testid='social_top_chirps_page']"
    * def social_menu_top_chirps_button = "[data-testid='social_menu_top_chirps_button']"
    * def social_user_profile_page = "[data-testid='social_user_profile_page']"
    * def social_chirp_input = "textarea[data-testid='new_chirp_input']"
    * def social_chirp_publish_button = "[data-testid='new_chirp_publish']"
    * def social_chirp_message = "[data-testid='chirp_message']"
    * def social_chirp_like_button = "[data-testid='thumbs-up']"
    * def social_chirp_like_count = "[data-testid='thumbs-up-count']"
    * def social_chirp_dislike_button = "[data-testid='thumbs-down']"
    * def social_chirp_dislike_count = "[data-testid='thumbs-down-count']"
    * def social_chirp_love_button = "[data-testid='heart']"
    * def social_chirp_love_count = "[data-testid='heart-count']"
    * def social_chirp_delete = "[data-testid='delete_chirp']"

    # Digital Cash screen
    * def digital_cash_coin_issuance_button = "[data-testid='digital-cash-coin-issuance']"
    * def digital_cash_first_roll_call_button = "[data-testid='digital-cash-roll-call-token-0']"
    * def digital_cash_amount_input = "input[data-testid='digital-cash-send-amount']"
    * def digital_cash_beneficiary_input = "input[data-testid='digital-cash-send-beneficiary']"
    * def digital_cash_beneficiary_select = "select"
    * def digital_cash_send_button = "{}Send Transaction"

  @name=open_app
  Scenario:
    Given driver webDriverOptions
    And driver 'about:blank'
    And driver.dimensions = { left: 0, top: 0, width: screenWidth, height: screenHeight }
    When driver frontendURL

  @name=create_new_wallet
  Scenario:
    Given call read('web.feature@name=open_app')
    When waitFor(wallet_new_wallet_button)
    And click(wallet_new_wallet_button)

  @name=restore_wallet
  Scenario:
    Given call read('web.feature@name=open_app')
    When waitFor(wallet_goto_restore_wallet_button).click()
    And input(wallet_restore_input, params.seed)
    And click(wallet_restore_wallet_button)

  @name=lao_join
  Scenario:
    Given call read('web.feature@name=create_new_wallet')
    When waitFor(lao_join_button).click()
    And waitFor(lao_enter_manually_button).click()
    And waitFor(lao_enter_manually_server_input).clear().input(serverURL)
    And waitFor(lao_enter_manually_lao_input).clear().input(params.lao.id)
    And click(lao_enter_manually_submit_button)
    Then waitFor(event_title)

  @name=lao_create
  Scenario:
    Given call read('web.feature@name=create_new_wallet')
    When waitFor(lao_create_button).click()
    And waitFor(lao_organization_name_input).input(organization_name)
    And waitFor(lao_server_url_input).clear().input(serverURL)
    Then click(lao_launch_button)

  @name=click_rollcall_create
  Scenario:
    * actionSheetClick(event_create_rollcall)

  @name=user_click
  Scenario:
    * waitFor("[data-testid='user_list_item_" + params.token + "']").click()

  @name=join_rollcall
  Scenario:
    Given def rollCall = params.organizer.createRollCall(lao)
    And organizer.openRollCall(lao, rollCall)
    And call read(PLATFORM_FEATURE) { name: '#(JOIN_LAO)', params: { lao: '#(params.lao)' } }
    When waitFor(event_first_current_event).click()
    And waitFor(event_rollcall_pop_token)
    And delay(1000)
    And def popToken = text(event_rollcall_pop_token)
    And organizer.closeRollCall(lao, rollCall, [popToken, organizer.publicKey])
    And delay(1000)

  @name=organizer_with_pop_token
  Scenario:
    Given call read(PLATFORM_FEATURE) { name: '#(CREATE_LAO)', params: { organization_name: 'Join roll call' } }
    And def rollCallName = 'My Roll-Call'
    When waitFor(event_create_button).click()
    And call read(PLATFORM_FEATURE) { name: '#(CLICK_CREATE_ROLLCALL)' }
    And waitFor(event_rollcall_name_input).input(rollCallName)
    And waitFor(event_rollcall_location_input).input('Between 1 and 0s')
    And waitFor(event_rollcall_confirm_button).click()
    Then waitForText(event_first_current_event, rollCallName)
    When waitFor(event_first_current_event).click()
    And waitFor(event_open_rollcall_button).click()
    And delay(500)
    And def popToken = text(event_rollcall_first_attendee)
    And waitFor(event_close_rollcall_button).click()

  @name=switch_to_social_page
  Scenario:
    Given waitFor(drawer_menu_button).click()
    And waitFor(drawer_menu_social).click()
    And delay(500)

  @name=switch_to_digital_cash_page
  Scenario:
    Given waitFor(drawer_menu_button).click()
    And waitFor(drawer_menu_digital_cash).click()
    And delay(500)
