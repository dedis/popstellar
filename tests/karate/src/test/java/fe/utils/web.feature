Feature: web test

  Background: App Preset
    * configure driver = { type: 'chrome', executable: 'C:/Program Files/Google/Chrome/Application/chrome.exe'}
    #* configure driver = { type: 'chrome' }
    * def driverOptions = karate.toAbsolutePath('file:../../fe1-web/web-build/index.html')

    # ================= Page Object Start ====================

    # Introduction screen
    * def exploring_selector = "[data-testid='exploring_selector']"

    #Home Screen
    * def tab_connect_selector = '{}Connect'
    * def block_camera_selector = '{}block'
    * def launch_selector = "[data-testid='launch_selector']"

    # Launch screen
    * def tab_launch_lao_name_selector = "input[data-testid='launch_organization_name_selector']"
    * def launch_address_selector = "input[data-testid='launch_address_selector']"
    * def tab_launch_create_lao_selector = "[data-testid='launch_launch_selector']"

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
    * click(exploring_selector)
    * click(tab_connect_selector)

    #refuse camera access

    * click(launch_selector)
    * input(launch_address_selector, backendURL)
