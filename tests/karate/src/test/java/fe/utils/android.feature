Feature: android page object

  Background: Android Preset
    * configure driver = { type: 'android', webDriverPath : "/wd/hub", start: false, httpConfig : { readTimeout: 120000 }}

    * def capabilities = android.desiredConfig
    # Replace the relative path to apk the absolute path
    * capabilities.app = karate.toAbsolutePath('file:' + capabilities.app)
    * def driverOptions = { webDriverSession: { desiredCapabilities : "#(capabilities)" } }

    # ================= Page Object Start ====================

    # Tab buttons
    * def tab_home_selector = '#com.github.dedis.popstellar:id/tab_home'
    * def tab_connect_selector = '#com.github.dedis.popstellar:id/tab_connect'
    * def tab_launch_selector = '#com.github.dedis.popstellar:id/tab_launch'
    * def tab_wallet_selector = '#com.github.dedis.popstellar:id/tab_wallet'

    # Launch tab
    * def tab_launch_lao_name_selector = '#com.github.dedis.popstellar:id/entry_box_launch'
    * def tab_launch_create_lao_selector = '#com.github.dedis.popstellar:id/button_launch'

    # Wallet tab
    * def tab_wallet_new_wallet_selector = '#com.github.dedis.popstellar:id/button_new_wallet'
    * def tab_wallet_confirm_selector = '#com.github.dedis.popstellar:id/button_confirm_seed'

    # Lao Event List
    * def add_event_selector = '#com.github.dedis.popstellar:id/add_future_event_button'

    * def lao_list = '#com.github.dedis.popstellar:id/lao_list'
    * def lao_list_2 = '{}Lao Name'

    * def add_event_selector = '#com.github.dedis.popstellar:id/add_future_event_button'

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

    * click(tab_wallet_selector)
    * click(tab_wallet_new_wallet_selector)
    * click(tab_wallet_confirm_selector)
    * dialog(true)

