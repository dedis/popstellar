Feature: android page object

  Background: Android Preset
    * configure driver = { type: 'android', webDriverPath : "/wd/hub", start: false, httpConfig : { readTimeout: 120000 }}

    * def capabilities = android.desiredConfig
    # Replace the relative path to apk the absolute path
    * capabilities.app = karate.toAbsolutePath('file:' + capabilities.app)
    * def driverOptions = { webDriverSession: { desiredCapabilities : "#(capabilities)" } }

    # ================= Page Object Start ====================

    # Tab buttons
    * def tab_home_selector = '#com.github.dedis.popstellar:id/home_home_menu'
    * def tab_connect_selector = '#com.github.dedis.popstellar:id/home_connect_menu'
    * def launch_selector = '#com.github.dedis.popstellar:id/home_launch_menu'
    * def tab_wallet_selector = '#com.github.dedis.popstellar:id/home_wallet_menu'

    # Launch tab
    * def tab_launch_lao_name_selector = '#com.github.dedis.popstellar:id/entry_box_launch'
    * def tab_launch_create_lao_selector = '#com.github.dedis.popstellar:id/button_launch'

    # Wallet tab
    * def tab_wallet_new_wallet_selector = '#com.github.dedis.popstellar:id/button_new_wallet'
    * def tab_wallet_confirm_selector = '#com.github.dedis.popstellar:id/button_confirm_seed'

    # Lao Event List
    * def add_event_selector = '#com.github.dedis.popstellar:id/add_event'
    * def add_roll_call_selector = '#com.github.dedis.popstellar:id/add_roll_call'
    * def roll_call_title_selector = '#com.github.dedis.popstellar:id/roll_call_title_text'
    * def roll_call_open_selector = '#com.github.dedis.popstellar:id/roll_call_open'
    * def roll_call_confirm_selector = '#com.github.dedis.popstellar:id/roll_call_confirm'
    * def roll_call_close_confirm_selector = '#com.github.dedis.popstellar:id/add_attendee_confirm'
    * def event_name_selector = '#com.github.dedis.popstellar:id/event_card_text_view'

    # Roll Call Screen
    * def roll_call_action_selector = '#com.github.dedis.popstellar:id/roll_call_management_button'
    * def roll_call_close_selector = '#com.github.dedis.popstellar:id/add_attendee_confirm'
    * def roll_call_manual_selector = '#com.github.dedis.popstellar:id/permission_manual_rc'
    * def allow_camera_selector = '#com.github.dedis.popstellar:id/allow_camera_button'
    * def manual_add_text_selector = '#com.github.dedis.popstellar:id/manual_add_edit_text'
    * def manual_add_confirm_selector = '#com.github.dedis.popstellar:id/manual_add_confirm'




  @name=basic_setup
  Scenario: Setup connection to the backend and complete wallet initialization
    Given driver driverOptions

    # Create and import mock backend
    * call read('classpath:fe/net/mockBackend.feature')
    * def backendURL = 'ws://10.0.2.2:' + backend.getPort()
    # Import message filters
    * call read('classpath:common/net/filters.feature')

    # As the settings tab does not have an id, this is how we click on it.
    # If this breaks, use this code to log the page hierarchy :
    # karate.log(driver.getHttp().path("source").get().value)
    And click('//*[@content-desc="More options"]')
    * click('#com.github.dedis.popstellar:id/title')

    # Input the mock backend url and connect to it
    * input('#com.github.dedis.popstellar:id/entry_box_server_url', backendURL)
    * click('#com.github.dedis.popstellar:id/button_apply')
    * match backend.waitForConnection(5000) == true

    # Initialize wallet
    * click(tab_wallet_selector)
    * click(tab_wallet_new_wallet_selector)
    * click(tab_wallet_confirm_selector)
    * dialog(true)

    * click(launch_selector)

  # Roll call create android procedure
  @name=create_roll_call
  Scenario: Create a roll call for an already created LAO
    When click(add_event_selector)
    And click(add_roll_call_selector)

    # Provide roll call information
    And input(roll_call_title_selector, constants.RC_NAME)

  # Roll call open android procedure
  @name=open_roll_call
  Scenario: Opens the created roll-call
    * click(event_name_selector)
    * backend.clearBuffer()
    * click(roll_call_action_selector)

  @name=close_roll_call
  Scenario: Closes a roll call with only the organizer attending
    # Close roll call
    * retry(5,200).click(roll_call_close_selector)
    * backend.clearBuffer()
    * dialog(true)


  @name=close_roll_call_w_attendees
  Scenario: Closes a roll call with 2 attendees and the organizer
    # Add attendees
    * input(manual_add_text_selector, token1)
    * click(manual_add_confirm_selector)
    * input(manual_add_text_selector, token2)
    * click(manual_add_confirm_selector)
    * backend.clearBuffer()

    # wait for popup
    * wait(3)
    # Close roll call
    * click(roll_call_close_selector)
    * dialog(true)

  #roll call open android procedure
  @name=reopen_roll_call
  Scenario: reopens the created roll-call
    * click(event_name_selector)
    * backend.clearBuffer()
    * click(roll_call_action_selector)
