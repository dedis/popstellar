Feature: Wallet
  Background:
  * call read('classpath:fe/utils/constants.feature')

  @name=open_app
  Scenario: Open the app for the first time and see the wallet seed
    Given call read(PLATFORM_FEATURES) { name: "#(OPEN_APP)" }
    When match text(wallet_seed_wallet_text) == "#regex ^([a-z]+\\s){11}[a-z]+$"
    Then screenshot()

  @name=wallet_create
  Scenario: Create a new wallet
    Given call read(PLATFORM_FEATURES) { name: "#(CREATE_NEW_WALLET)" }
    When waitFor(lao_join_button)
    Then screenshot()

  @name=wallet_restore
  Scenario: Restore a wallet
    Given call read(PLATFORM_FEATURES) {name: "#(RESTORE_WALLET)", params: { seed: 'present guilt frost screen fabric rotate citizen decide have message chat hood' } }
    When waitFor(lao_join_button)
    Then screenshot()
