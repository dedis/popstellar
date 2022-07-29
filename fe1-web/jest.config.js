module.exports = {
  preset: 'jest-expo',
  moduleFileExtensions: ['ts', 'tsx', 'js', 'jsx', 'json', 'node'],
  setupFilesAfterEnv: ["./jest/setupAfterEnv.js", "jest-extended"],
  testPathIgnorePatterns: ["/node_modules/"],
  testMatch: [ "**/?(*.)+(spec|test).[jt]s?(x)" ],
  moduleNameMapper: {
    "^test_data/(.*)$": "<rootDir>/../tests/data/$1",
    "^protocol/(.*)$": "<rootDir>/../protocol/$1",
    '\\.(css|less)$': '<rootDir>/jest/styleMock.js',
  },
  collectCoverage: true,
  testResultsProcessor: "jest-sonar-reporter",
  setupFiles: ["./jest/setup.js"],
  transformIgnorePatterns: [
    "node_modules/(?!((jest-)?react-native|@react-native(-community)?)|expo(nent)?|@expo(nent)?/.*|@expo-google-fonts/.*|react-navigation|@react-navigation/.*|@unimodules/.*|unimodules|sentry-expo|native-base|react-native-svg)"
  ]
};
