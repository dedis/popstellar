@ignore @report=false
Feature: web page object

  @name=basic_setup
  Scenario:
    Given driver webDriverOptions
    Given driver 'about:blank'
    And driver.dimensions = { left: 0, top: 0, width: screenWidth, height: screenHeight }
    Then driver frontendURL
    And delay(1000)
