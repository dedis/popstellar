const path = require('path');

module.exports = (api) => {
  api.cache(true);
  return {
    presets: ['babel-preset-expo'],
    plugins: [
      [
        'module-resolver',
        {
          root: ['./src'],
          extensions: ['.ios.js', '.android.js', '.js', '.ts', '.tsx', '.json'],
          alias: {
            test_data: path.resolve(__dirname, '../tests/data'),
            protocol: path.resolve(__dirname, '../protocol'),
          }
        },
      ],
      '@babel/plugin-proposal-export-namespace-from',
      // Reanimated plugin has to be listed last (https://docs.swmansion.com/react-native-reanimated/docs/fundamentals/installation/)
      'react-native-reanimated/plugin'
    ],
  };
};
