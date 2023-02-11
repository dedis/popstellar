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
          // issue is resolved.
          // Update (2023-02-11, Tyratox): ^4.0.0-rc.7 fixes this issue but some other issue causes the icons
          // to not render properly, hence I had to revert the changes in 203194cc9ac6126fbbe03aae3efa69740ff15e7a
          dangerouslyAddModulePathsToTranspile: ['@rneui/base', '@rneui/themed'],
      },
  }, argv);

  config.resolve.alias = {
    ...config.resolve.alias,
    ...aliases,
    // make sure only a single version of react is loaded.
    // at the moment (2022-12-06) react-camera otherwise loads a second version of react
    // making the application crash
    react: path.resolve('./node_modules/react')
  };

  return config;
}

module.exports = configAsync;
