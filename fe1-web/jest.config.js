module.exports = {
  preset: 'react-native',
  moduleFileExtensions: ['ts', 'tsx', 'js', 'jsx', 'json', 'node'],
  setupFilesAfterEnv: ["jest-extended"],
  testPathIgnorePatterns: ["/node_modules/"],
  testMatch: [ "**/__tests__/**/?(*.)+(spec|test).[jt]s?(x)", "**/?(*.)+(spec|test).[jt]s?(x)" ],
};
