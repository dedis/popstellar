@ignore @report=false
Feature: Constants
  Scenario: Creates constants that will be used by other features
    * def PLATFORM_FEATURE = 'classpath:fe/utils/platform.feature'
    * def MOCK_CLIENT_FEATURE = 'classpath:fe/utils/mock_client.feature'
    * def OPEN_APP = 'open_app'
    * def CREATE_NEW_WALLET = 'create_new_wallet'
    * def RESTORE_WALLET = 'restore_wallet'
