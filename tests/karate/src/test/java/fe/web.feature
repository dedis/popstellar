Feature: web test

  Background: App Preset
    * configure driver = { type: 'chrome', executable: 'C:/Program Files/Google/Chrome/Application/Chrome.exe', showDriverLog: true  }

  Scenario: web app UI tests
    Given driver '../../fe1-web/web-build/index.html'
    When driver.click('')
    And driver.input('input[placeholder=Organization name]', 'Lao')
    And driver.input('input[placeholder=Address]', 'localhost:9000')
    Then match driver.text('input[placeholder=Address]') == 'localhost:9000'
