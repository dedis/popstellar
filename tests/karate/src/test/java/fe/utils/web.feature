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

    # Lao Event List
    * def past_header_selector = '{^}Past'
    * def add_event_selector = "[data-testid='create_event_selector']"
    * def tab_events_selector = '{}Events'
    * def roll_call_title_selector = "input[data-testid='roll_call_name_selector']"
    * def roll_call_location_selector = "input[data-testid='roll_call_location_selector']"
    * def roll_call_confirm_selector = "[data-testid='roll_call_confirm_selector']"
    * def event_name_selector = '{}RC name'

    # Roll Call Screen
    * def roll_call_option_selector = "[data-testid='roll_call_options']"
    * def roll_call_stop_scanning_selector = "[data-testid='roll-call-open-stop-scanning']"
    * def roll_call_manual_selector = "[data-testid='roll-call-open-add-manually']"
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
    * def wait =
            """
                function(secs) {
                    java.lang.Thread.sleep(secs*1000)
                }
            """

    * click(exploring_selector)
    * click(tab_connect_selector)

    #refuse camera access

    * click(launch_selector)
    * input(launch_address_selector, backendURL)

  #roll call web procedure
  @name=create_roll_call
  Scenario: Create a roll call for an already created LAO
    Given click(tab_events_selector)
    And click(add_event_selector)

    # Clicking on Create Roll-Call
    * script("setTimeout(() => document.evaluate('//div[text()=\\'Create Roll-Call\\']', document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue.click(), 1000)")

    And retry(20 ,200).input(roll_call_title_selector, 'RC name')
    And input(roll_call_location_selector, 'EPFL')

    #roll call open web procedure
  @name=open_roll_call
  Scenario: Opens the created roll-call
    * retry(5,1000).click(event_name_selector)
    * click(roll_call_option_selector)
    * script("setTimeout(() => document.evaluate('//div[text()=\\'Open Roll-Call\\']', document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue.click(), 1000)")
    # needed to work
    * wait(2)

  @name=close_roll_call
  Scenario: Closes a roll call with only the organizer attending
    * click(roll_call_option_selector)
    # We need to start scanning for the organizer token to be added
    * script("setTimeout(() => document.evaluate('//div[text()=\\'Scan Attendees\\']', document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue.click(), 1000)")
    * wait(2)
    * click(roll_call_stop_scanning_selector)
    * click(roll_call_option_selector)
    * script("setTimeout(() => document.evaluate('//div[text()=\\'Close Roll-Call\\']', document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue.click(), 1000)")
    # needed to work
    * wait(2)

  @name=reopen_roll_call
  Scenario: Reopen a closed roll call
    * click(past_header_selector)
    * retry(5,1000).click(event_name_selector)
    * click(roll_call_option_selector)

    * script("setTimeout(() => document.evaluate('//div[text()=\\'Re-open Roll-Call\\']', document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue.click(), 1000)")
