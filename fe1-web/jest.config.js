module.exports = {
  preset: 'jest-expo',
  //preset: 'react-native',
  moduleFileExtensions: ['ts', 'tsx', 'js', 'jsx', 'json', 'node'],
  setupFilesAfterEnv: ["jest-extended"],
  testPathIgnorePatterns: ["/node_modules/"],
  testMatch: [ "**/?(*.)+(spec|test).[jt]s?(x)" ],
  moduleNameMapper: {
    "^test_data/(.*)$": "<rootDir>/../tests/data/$1",
    "^protocol/(.*)$": "<rootDir>/../protocol/$1",
  },
  collectCoverage: true,
  testResultsProcessor: "jest-sonar-reporter",
  setupFiles: ["./jest/setup.js"],
  // setupFiles: ['./node_modules/react-native-gesture-handler/jestSetup.js'],
  transformIgnorePatterns: [
    "node_modules/(?!((jest-)?react-native|@react-native(-community)?)|expo(nent)?|@expo(nent)?/.*|@expo-google-fonts/.*|react-navigation|@react-navigation/.*|@unimodules/.*|unimodules|sentry-expo|native-base|react-native-svg)"
  ],
/*  transformIgnorePatterns: [
    'node_modules/(?!(jest-)?@?react-native|@react-native-community|@react-navigation)',
  ],*/
};

// https://callstack.github.io/react-native-testing-library/docs/react-navigation/
