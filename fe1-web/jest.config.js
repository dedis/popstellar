module.exports = {
  preset: 'react-native',
  moduleFileExtensions: ['ts', 'tsx', 'js', 'jsx', 'json', 'node'],
  setupFilesAfterEnv: ["jest-extended"],
  testPathIgnorePatterns: ["/node_modules/"],
  testMatch: [ "**/?(*.)+(spec|test).[jt]s?(x)" ],
};
