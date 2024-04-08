Feature: Wallet
  @name=open_app
  Scenario: Open the app for the first time and see the wallet seed
    Given call read('classpath:fe/utils/platform.feature') { name: 'open_app' }
    When match text(wallet_seed_wallet_text) == "#regex ^([a-z]+\\s){11}[a-z]+$"
    Then screenshot()

  @name=wallet_create
  Scenario: Create a new wallet
    Given call read('classpath:fe/utils/platform.feature') { name: 'create_new_wallet' }
    When waitFor(lao_join_button)
    Then screenshot()

  @name=wallet_restore
  Scenario: Restore a wallet
    Given call read('classpath:fe/utils/platform.feature') {name: 'restore_wallet', params: { seed: 'present guilt frost screen fabric rotate citizen decide have message chat hood' } }
    When waitFor(lao_join_button)
    Then screenshot()

