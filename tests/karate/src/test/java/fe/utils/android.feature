@ignore @report=false
Feature: android page object
  Background:
    # Wallet screen
    * def wallet_button_empty_ok = '//*[@text="OK"]'
    * def wallet_seed_wallet_text = '#com.github.dedis.popstellar:id/seed_wallet_text'
    * def wallet_new_wallet_button = "#com.github.dedis.popstellar:id/button_confirm_seed"
    * def wallet_confirm_new_wallet_button = '//*[@text="YES"]'
    * def wallet_restore_button = '#com.github.dedis.popstellar:id/import_seed_button'
    * def wallet_restore_input = '#com.github.dedis.popstellar:id/import_seed_entry_edit_text'

    # Lao screen
    * def lao_create_button = "#com.github.dedis.popstellar:id/home_create_button"
    * def lao_join_button = '#com.github.dedis.popstellar:id/home_join_button'
    * def lao_organization_name_input = "#com.github.dedis.popstellar:id/lao_name_entry_edit_text"
    * def lao_server_url_input = "#com.github.dedis.popstellar:id/server_url_entry_edit_text"
    * def lao_launch_button = "#com.github.dedis.popstellar:id/button_create"
    * def lao_enter_manually_button = "#com.github.dedis.popstellar:id/scanner_enter_manually"
    * def lao_enter_manually_lao_input = "#com.github.dedis.popstellar:id/manual_add_edit_text"
    * def lao_enter_manually_submit_button = "#com.github.dedis.popstellar:id/manual_add_button"

    # Event screen
    * def event_create_button = "#com.github.dedis.popstellar:id/add_event"
    * def event_create_rollcall = "#com.github.dedis.popstellar:id/add_roll_call"
    * def event_title = '//android.widget.TextView[@text="Events"]'
    * def event_rollcall_name_input = "#com.github.dedis.popstellar:id/roll_call_title_text"
    * def event_rollcall_location_input = "#com.github.dedis.popstellar:id/roll_call_event_location_text"
    * def event_rollcall_confirm_button = "#com.github.dedis.popstellar:id/roll_call_confirm"
    * def event_rollcall_pop_token = "#com.github.dedis.popstellar:id/roll_call_pop_token_text"
    * def event_rollcall_first_attendee = "#android:id/text1"
    * def event_first_current_event = "#com.github.dedis.popstellar:id/event_card_text_view"

  @name=open_app
  Scenario:
    Given driver webDriverOptions
    When waitFor(wallet_button_empty_ok).click()

  @name=create_new_wallet
  Scenario:
    Given call read('android.feature@name=open_app')
    When waitFor(wallet_new_wallet_button)
    And click(wallet_new_wallet_button)
    And waitFor(wallet_confirm_new_wallet_button).click()

  @name=restore_wallet
  Scenario:
    Given call read('android.feature@name=open_app')
    When input(wallet_restore_input, params.seed)
    And click(wallet_restore_button)

  @name=lao_join
  Scenario:
    Given call read('android.feature@name=create_new_wallet')
    When waitFor(lao_join_button).click()
    And waitFor(lao_enter_manually_button).click()
    # For some reason, karate always converts json strings to javascript objects when passed as parameters to the input function.
    # This workaround is the only solution I found to pass a json string as a parameter to the input field.
    # This should be fixed once multiple fields are used instead of a single field accepting json.
    And json jsonArray = ['{"lao":"', "#(params.lao.id)", '","server":"', "#(platformServerURL)", '"}']
    And string jsonString = jsonArray
    And input(lao_enter_manually_lao_input, jsonString)
    And click(lao_enter_manually_submit_button)
    Then waitFor(event_title)

  @name=lao_create
  Scenario:
    Given call read('android.feature@name=create_new_wallet')
    When waitFor(lao_create_button).click()
    And waitFor(lao_organization_name_input).input(organization_name)
    And waitFor(lao_server_url_input).clear().input(platformServerURL)
    Then click(lao_launch_button)

  @name=click_rollcall_create
  Scenario:
    * waitFor(event_create_rollcall).click()
