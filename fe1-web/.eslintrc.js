module.exports = {
  root: true,

  parser: '@typescript-eslint/parser',

  parserOptions: {
    ecmaFeatures: {
      jsx: true
    },
    ecmaVersion: 2021,
    project: ['./tsconfig.json']
  },

  extends: [
    './.eslint.react-native-config.js',
    'airbnb',
    'airbnb-typescript',
    'plugin:import/recommended',
    'plugin:import/typescript',
    'plugin:prettier/recommended',
    // The following rule set is desirable but requires significant adaptations to the code base.
    // Consider adding it once (1) all previous rules are already satisfied an
    // (2) `any`s have been removed from the code base in favor of typed values and `unknown`s
    // 'plugin:@typescript-eslint/recommended'
  ],

  plugins: [
    'import',
  ],

  settings: {
    'import/parsers': {
      '@typescript-eslint/parser': ['.ts', '.tsx']
    },
    'import/resolver': {
      node: {
        extensions: ['.mjs', '.js', '.jsx', '.ts', '.tsx', '.json'],
        moduleDirectory: ['node_modules', 'src'],
      },
    },
    react: {
      version: 'detect'
    }
  },

  rules: {
    // Exclude aliases from unresolved imports, due to the limitations of eslint-plugin-import
    // cf. https://github.com/import-js/eslint-plugin-import/issues/496
    'import/no-unresolved': ['error', { ignore: [ 'test_data', 'protocol' ] }],

    // This rule excludes testing files from the requirement to only rely on "dependencies"
    // Imports in testing files relying on "devDependencies" should be acceptable.
    'import/no-extraneous-dependencies': [
      'error', {'devDependencies': [
          '**/__tests__/**/*',
          '**/__mocks__/**/*',
          'jest/**/*.js'
      ]}],

    'import/order': [
      'error', {
        'groups': ['builtin', 'external', 'internal', ['parent', 'sibling', 'index'], 'type', 'object'],
        'newlines-between': 'always',
        'alphabetize': { order: 'asc', caseInsensitive: true }
      }
    ],

    // allow JSX code only in files with the correct extensions
    'react/jsx-filename-extension': ['error', { extensions: ['.jsx', '.tsx'] }],

    // do not require a default exporter if there's only one export in the file (e.g., in index.ts)
    // https://github.com/import-js/eslint-plugin-import/blob/main/docs/rules/prefer-default-export.md
    'import/prefer-default-export': 'off',

    // allow the usage of console output for debugging purposes
    'no-console': 'off',

    // do not require object shorthand usage
    // cf. https://eslint.org/docs/rules/object-shorthand
    'object-shorthand': 'off',

    // disable some highly error-prone and confusing ECMAScript syntax and statements
    // cf. https://eslint.org/docs/rules/no-restricted-syntax
    'no-restricted-syntax': [
      'error',
      {
        selector: 'ForInStatement',
        message: 'for..in loops iterate over the entire prototype chain, which is virtually never what you want. Use Object.{keys,values,entries}, and iterate over the resulting array.',
      },
      {
        selector: 'LabeledStatement',
        message: 'Labels are a form of GOTO; using them makes code confusing and hard to maintain and understand.',
      },
      {
        selector: 'WithStatement',
        message: '`with` is disallowed in strict mode because it makes code impossible to predict and optimize.',
      },
    ],

    "react-native/no-inline-styles": 'error',
    "react-native/no-unused-styles": 'error',
    "react-native/no-single-element-style-arrays": 'error',
    // enforce the use of color variables. this should make programmers use the defined color palette
    // or extend it if really necessary but not add new colors for every new component
    "react-native/no-color-literals": 'error',
  },
}
