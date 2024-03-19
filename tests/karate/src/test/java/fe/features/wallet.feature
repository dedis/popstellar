Feature: Wallet

  Scenario: Create a new wallet
    * driver 'about:blank'
    * driver.dimensions = { x: 0, y: 0, width: screenWidth, height: screenHeight }
    * driver frontendURL
    * waitFor("[data-testid='exploring_selector']").click()
    * delay(1000)
    * def texts = scriptAll('div', '_.textContent')
    * match texts contains '#regex^([a-z]+\\s){11}[a-z]+$'
    * delay(1000).screenshot()





