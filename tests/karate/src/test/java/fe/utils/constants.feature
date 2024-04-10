@ignore @report=false
Feature: Constants
  Scenario: Creates constants that will be used by other features
    * def PLATFORM_FEATURES = 'classpath:fe/utils/platform.feature'
    * def OPEN_APP = 'open_app'
    * def CREATE_NEW_WALLET = 'create_new_wallet'
    * def RESTORE_WALLET = 'restore_wallet'
