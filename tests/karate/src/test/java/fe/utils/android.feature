@ignore @report=false
Feature: android page object
  Background: Android Preset
    # Wallet screen
    * def wallet_button_empty_ok = '//*[@text="OK"]'

  @name=basic_setup
  Scenario:
    Given driver webDriverOptions
    Then waitFor(wallet_button_empty_ok).click()
