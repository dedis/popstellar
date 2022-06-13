Feature: web test

  Background: App Preset
    * configure driver = { type: 'chrome', executable: 'C:/Program Files/Google/Chrome/Application/chrome.exe'}
    #* configure driver = { type: 'chrome' }
    * def driverOptions = karate.toAbsolutePath('file:../../fe1-web/web-build/index.html')

    # ================= Page Object Start ====================

    # Tab buttons
    * def tab_home_selector = '{}Home'
    * def tab_connect_selector = '{}Connect'
    * def tab_launch_selector = '{}Launch'
    * def tab_wallet_selector = '{}Wallet'

    # Launch tab
    * def tab_launch_lao_name_selector = "input[placeholder='Organization name']"
    * def tab_launch_address_selector = "input[placeholder='Address']"
    * def tab_launch_create_lao_selector = '{}Launch -- Connect, Create LAO & Open UI'

  @name=basic_setup
  Scenario: Setup connection to the backend and complete on the home page
    Given driver driverOptions

    # Create and import mock backend
    And call read('classpath:fe/net/mockBackend.feature')
    * def backendURL = 'ws://localhost:' + backend.getPort()
    # Import message filters
    And call read('classpath:common/net/filters.feature')

    # The default input function is not consistent and does not work every time.
    # This replaces the input function with one that just tries again until it works.
    * def input =
          """
            function(selector, data) {
              tries = 0
              while (driver.attribute(selector, "value") != data) {
                if (tries++ >= max_input_retry)
                  throw "Could not input " + data + " - max number of retry reached."
                driver.clear(selector)
                driver.input(selector, data)
                delay(10)
              }
            }
          """

    And click(tab_launch_selector)
    And input(tab_launch_address_selector, backendURL)
    And click(tab_home_selector)
