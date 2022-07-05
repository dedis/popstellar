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
    * def launch_selector = "[data-testid='launch_selector']"

    # Launch screen
    * def tab_launch_lao_name_selector = "input[data-testid='launch_organization_name_selector']"
    * def backend_address_selector = "input[data-testid='launch_address_selector']"
    * def tab_launch_create_lao_selector = "[data-testid='launch_launch_selector']"

    # Lao Event List
    * def past_header_selector = '{^}Past'
    * def add_event_selector = "[data-testid='create_event_selector']"
    * def tab_events_selector = '{}Events'
    * def roll_call_title_selector = "input[data-testid='roll_call_name_selector']"
    * def roll_call_location_selector = "input[data-testid='roll_call_location_selector']"
    * def roll_call_confirm_selector = "[data-testid='roll_call_confirm_selector']"
    * def event_name_selector = "[data-testid='current_event_selector_0']"

    # Roll Call Screen
    * def roll_call_option_selector = "[data-testid='roll_call_options']"
    * def roll_call_stop_scanning_selector = "[data-testid='roll_call_open_stop_scanning']"
    * def roll_call_manual_selector = "[data-testid='roll_call_open_add_manually']"
    * def manual_add_description_selector = '{^}Enter token:'
    * def manual_add_confirm_selector = '{}Add'
    * def manual_add_done_selector = '{}Done'

    # Election
    * def election_name_selector = "[data-testid='election_name_selector']"
    * def election_question_selector = "[data-testid='question_selector_0']"
    * def election_ballot_selector_1 = "[data-testid='question_0_ballots_option_0_input']"
    * def election_ballot_selector_2 = "[data-testid='question_0_ballots_option_1_input']"
    * def election_confirm_selector = "[data-testid='election_confirm_selector']"
    * def election_event_selector = '{^}Election'
    * def election_option_selector = "[data-testid='election_option_selector']"



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
    # Click on the connect navigation item
    * retry(5,1000).click(tab_connect_selector)
    # Click on launch button
    * click(launch_selector)
    # Connect to the backend
    * input(backend_address_selector, backendURL)

  # Roll call create web procedure
  @name=create_roll_call
  Scenario: Create a roll call for an already created LAO
    Given retry(10, 200).click(tab_events_selector)
    And click(add_event_selector)

    # Clicking on Create Roll-Call. This is because it is (as of now) an actionSheet element which does not have an id
    # If it breaks down, check that the name of the button has not changed, try to add more delay. Otherwise maybe karate
    # added a way to directly do that after the time of our writing.
    #
    # script allows the evaluation of arbitrary javascript code and document.evaluate
    # (https://developer.mozilla.org/en-US/docs/Web/API/Document/evaluate) allows the evaluation of an XPath expression.
    #
    # Somehow this turned out to work, at least if it was wrapped
    # in a setTimeout which delays the execution of the script.
    # The XPath selector is described here: https://stackoverflow.com/a/29289196/2897827
    * script("setTimeout(() => document.evaluate('//div[text()=\\'Create Roll-Call\\']', document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue.click(), 1000)")

    # Provide roll call required information
    And retry(5, 1000).input(roll_call_title_selector, constants.RC_NAME)
    And input(roll_call_location_selector, 'EPFL')

  # Roll call open web procedure
  @name=open_roll_call
  Scenario: Opens the created roll-call
    * retry(5,1000).click(event_name_selector)
    * retry(5,1000).click(roll_call_option_selector)
    * backend.clearBuffer()
    * script("setTimeout(() => document.evaluate('//div[text()=\\'Open Roll-Call\\']', document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue.click(), 500)")

  @name=close_roll_call
  Scenario: Closes a roll call with only the organizer attending
    * wait(1)
    * retry(5,1000).click(roll_call_option_selector)
    # We need to start scanning for the organizer token to be added
    * script("setTimeout(() => document.evaluate('//div[text()=\\'Scan Attendees\\']', document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue.click(), 1000)")
    * retry(5,1000).click(roll_call_stop_scanning_selector)
    * backend.clearBuffer()
    * click(roll_call_option_selector)
    * script("setTimeout(() => document.evaluate('//div[text()=\\'Close Roll-Call\\']', document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue.click(), 900)")
    # needed to work
    * wait(1)

  @name=close_roll_call_w_attendees
  Scenario: Closes a roll call with 2 attendees and the organizer
    * wait(1)
    * retry(5,1000).click(roll_call_option_selector)
    # We need to start scanning for the organizer token to be added
    * script("setTimeout(() => document.evaluate('//div[text()=\\'Scan Attendees\\']', document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue.click(), 1000)")
    * retry(5,1000).click(roll_call_manual_selector)

    # Add attendees
    * below(manual_add_description_selector).input(token1)
    * click(manual_add_confirm_selector)
    * below(manual_add_description_selector).clear()
    * below(manual_add_description_selector).input(token2)
    * click(manual_add_confirm_selector)
    * click(manual_add_done_selector)

    * retry(5,1000).click(roll_call_stop_scanning_selector)
    * backend.clearBuffer()
    * click(roll_call_option_selector)
    * script("setTimeout(() => document.evaluate('//div[text()=\\'Close Roll-Call\\']', document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue.click(), 1000)")
    # needed to work
    * wait(2)

  @name=reopen_roll_call
  Scenario: Reopen a closed roll call
    * click(past_header_selector)
    * retry(5,1000).click(event_name_selector)
    * wait(1)
    * click(roll_call_option_selector)
    * backend.clearBuffer()
    * script("setTimeout(() => document.evaluate('//div[text()=\\'Re-open Roll-Call\\']', document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue.click(), 1000)")
    * wait(2)

  # Election setup web procedure
  @name=setup_election
  Scenario: create election
    And click(add_event_selector)
    * script("setTimeout(() => document.evaluate('//div[text()=\\'Create Election\\']', document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue.click(), 1000)")
    * wait(1)
    * retry(5, 1000).input(election_name_selector, constants.ELECTION_NAME)
    * input(election_question_selector, constants.QUESTION_CONTENT)
    * input(election_ballot_selector_1, constants.BALLOT_1)
    * input(election_ballot_selector_2, constants.BALLOT_2)
    * backend.clearBuffer()
    * click(election_confirm_selector)
    * wait(2)

  # Election open web procedure
  @name=open_election
  Scenario: open election
    retry(5,1000).click('{^}Election')
    * retry(5,1000).click(election_option_selector)
    * backend.clearBuffer()
    * script("setTimeout(() => document.evaluate('//div[text()=\\'Open Election\\']', document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue.click(), 1000)")
