Feature: Wallet
  Background:
    * call read('classpath:fe/utils/constants.feature')

  Scenario: Open the app for the first time and see the wallet seed
    When call read(PLATFORM_FEATURES) { name: "#(OPEN_APP)" }
    Then match text(wallet_seed_wallet_text) == "#regex ^([a-z]+\\s){11}[a-z]+$"
    And screenshot()
