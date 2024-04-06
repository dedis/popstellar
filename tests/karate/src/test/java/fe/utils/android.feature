@ignore @report=false
Feature: android page object
  Background:
    # Wallet screen
    * def wallet_button_empty_ok = '//*[@text="OK"]'
    * def wallet_seed_wallet_text = '#com.github.dedis.popstellar:id/seed_wallet_text'
    * def wallet_new_wallet_button = "#com.github.dedis.popstellar:id/button_confirm_seed"
    * def wallet_confirm_new_wallet_button = '//*[@text="YES"]'
    # Lao screen
    * def lao_join_button = '#com.github.dedis.popstellar:id/home_join_button'

  @name=open_app
  Scenario:
    Given driver webDriverOptions
    Then waitFor(wallet_button_empty_ok).click()

  @name=create_new_wallet
  Scenario:
    Given call read('android.feature@name=open_app')
    When waitFor(wallet_new_wallet_button)
    Then click(wallet_new_wallet_button)
    Then waitFor(wallet_confirm_new_wallet_button).click()
