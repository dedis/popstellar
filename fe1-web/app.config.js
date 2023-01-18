const { execSync } = require('child_process');

const gitHash = execSync('git rev-parse HEAD', { encoding: 'ascii' }).trim();
const isDirty = execSync("git diff --quiet || echo '-dirty'", { encoding: 'ascii' }).trim();

module.exports = {
  expo: {
    name: 'PopApp',
    slug: 'PopApp',
    version: '1.0.0',
    orientation: 'portrait',
    icon: './src/resources/assets/icon.png',
    splash: {
      image: './src/resources/assets/splash.png',
      resizeMode: 'contain',
      backgroundColor: '#ffffff',
    },
    updates: {
      fallbackToCacheTimeout: 0,
    },
    assetBundlePatterns: ['./src/resources/**/*'],
    ios: {
      supportsTablet: true,
    },
    web: {
      favicon: './src/resources/assets/favicon.png',
    },
    entryPoint: './src/App.tsx',
    extra: {
      commitHash: `${gitHash}${isDirty}`,
      appVersion: process.env.APP_VERSION || 'v0.0.0',
      buildURL: process.env.APP_BUILD_URL || '#',
      shortSHA: process.env.APP_SHORT_SHA || `${gitHash}${isDirty}`,
      buildDate: process.env.APP_BUILD_DATE || '1.1.1970',
    },
  },
};
