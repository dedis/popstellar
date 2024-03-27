@ignore @report=false
Feature: android page object
  Background:
    # Wallet screen
    * def wallet_button_empty_ok = '//*[@text="OK"]'
    * def wallet_seed_wallet_text = '#com.github.dedis.popstellar:id/seed_wallet_text'

  @name=open_app
  Scenario:
    Given driver webDriverOptions
    Then waitFor(wallet_button_empty_ok).click()
