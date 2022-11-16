const createExpoWebpackConfigAsync = require('@expo/webpack-config');
const path = require('path');

const aliases = {
  test_data: path.resolve(__dirname, '../tests/data'),
  protocol: path.resolve(__dirname, '../protocol'),
};

async function configAsync(env, argv) {
  const config = await createExpoWebpackConfigAsync({
      ...env,
      babel: {
          // This is a workaround for https://github.com/react-native-elements/react-native-elements/issues/3607
          // to get the new version of react-native-elements working
          // Revert this commit as soon as the new version is properly released and this
          // issue is resolved
          dangerouslyAddModulePathsToTranspile: ['@rneui/base', '@rneui/themed'],
      },
  }, argv);

  config.resolve.alias = {
    ...config.resolve.alias,
    ...aliases,
  };

  return config;
}

module.exports = configAsync;
