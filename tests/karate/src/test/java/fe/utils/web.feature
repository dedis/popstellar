Feature: web test

  Background: App Preset
    * configure driver = { type: 'chrome' }
    * def driverOptions = karate.toAbsolutePath('file:../../fe1-web/web-build/index.html')
    # Create and import mock backend
    * call read('classpath:fe/net/mockbackend.feature')
    * def backendURL = 'ws://localhost:' + backend.getPort()
    # Import message filters
    * call read('classpath:common/net/filters.feature')

    # Tab buttons
    * def tab_home_selector = '{}Home'
    * def tab_connect_selector = '{}Connect'
    * def tab_launch_selector = '{}Launch'
    * def tab_wallet_selector = '{}Wallet'

    # Launch tab
    * def tab_launch_lao_name_selector = "input[placeholder='Organization name']"
    * def tab_launch_address_selector = "input[placeholder='Address']"
    * def tab_launch_create_lao_selector = '{}Launch -- Connect, Create LAO & Open UI'

  Scenario: web app UI tests
    * driver driverOptions
    * driver.click(tab_launch_selector)
    * delay(500)
    * driver.input(tab_launch_address_selector, backendURL)
    * driver.click(tab_home_selector)
    * delay(500)
