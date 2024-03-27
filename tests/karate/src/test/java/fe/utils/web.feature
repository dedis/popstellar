@ignore @report=false
Feature: web page object
  Background:
    # Wallet screen
    * def wallet_seed_wallet_text = "[data-testid='seed_wallet_text']"

  @name=open_app
  Scenario:
    Given driver webDriverOptions
    Given driver 'about:blank'
    And driver.dimensions = { left: 0, top: 0, width: screenWidth, height: screenHeight }
    Then driver frontendURL
    And delay(1000)
