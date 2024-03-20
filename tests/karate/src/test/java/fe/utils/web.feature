@ignore @report=false
Feature: web page object

  @name=basic_setup
  Scenario:
    Given driver 'about:blank'
    And driver.dimensions = { x: 0, y: 0, width: screenWidth, height: screenHeight }
    Then driver frontendURL
    And delay(1000)
