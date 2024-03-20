Feature: Wallet

  Scenario: Open the app for the first time and see the wallet seed
    When call read('classpath:fe/utils/platform.feature') { name: 'open_app' }
    Then match text(wallet_seed_wallet_text) == "#regex ^([a-z]+\\s){11}[a-z]+$"
    And screenshot()
