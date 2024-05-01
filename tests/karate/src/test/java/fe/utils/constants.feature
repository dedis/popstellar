@ignore @report=false
Feature: Constants
  Scenario: Creates constants that will be used by other features
    # Features
    * def PLATFORM_FEATURE = 'classpath:fe/utils/platform.feature'
    * def MOCK_CLIENT_FEATURE = 'classpath:fe/utils/mock_client.feature'
    * def LAO_FEATURE = 'classpath:fe/features/lao.feature'
    # Wallet
    * def OPEN_APP = 'open_app'
    * def CREATE_NEW_WALLET = 'create_new_wallet'
    * def RESTORE_WALLET = 'restore_wallet'
    # Lao
    * def JOIN_LAO = 'lao_join'
    * def CREATE_LAO = 'lao_create'
    # Event
    * def CLICK_CREATE_ROLLCALL = 'click_rollcall_create'
    * def JOIN_ROLLCALL = 'join_rollcall'
    * def SWITCH_TO_SOCIAL_PAGE = 'switch_to_social_page'
