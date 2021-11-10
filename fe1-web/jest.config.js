module.exports = {
  preset: 'react-native',
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
/*  transform: {
    "node_modules/@expo/vector-icons/.+\\.tsx?$": "ts-jest"
  },
  transformIgnorePatterns: [
    "node_modules/(?!@expo/vector-icons/.*)"
  ]*/
/*  transform: {
    "node_modules/createIconSet/.+\\.tsx?$": "ts-jest"
  },
  transformIgnorePatterns: [
    "node_modules/(?!createIconSet/.*)"
  ]
  transform: {
    "node_modules/react-native-vector-icons/.+\\.tsx?$": "ts-jest"
  },
  transformIgnorePatterns: [
    "node_modules/(?!react-native-vector-icons/.*)"
  ]*/
};

