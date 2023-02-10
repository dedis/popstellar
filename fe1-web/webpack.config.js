const createExpoWebpackConfigAsync = require('@expo/webpack-config');
const path = require('path');

const aliases = {
  test_data: path.resolve(__dirname, '../tests/data'),
  protocol: path.resolve(__dirname, '../protocol'),
};

async function configAsync(env, argv) {
  const config = await createExpoWebpackConfigAsync({
      ...env,
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
