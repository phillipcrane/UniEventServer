module.exports = {
  env: {
    es6: true,
    node: true,
  },
  parserOptions: {
    "ecmaVersion": 2022,
  },
  extends: [
    "eslint:recommended",
    "google",
  ],
  rules: {
    "no-restricted-globals": ["error", "name", "length"],
    "prefer-arrow-callback": "error",
    "quotes": "off",
    "linebreak-style": "off",
    "require-jsdoc": "off",
    "max-len": "off",
    "no-trailing-spaces": "off",
    "comma-dangle": "off",
    "object-curly-spacing": "off",
    "eol-last": "off",
    "indent": "off",
    "arrow-parens": "off",
    "valid-jsdoc": "off",
    // Ignore these additional stylistic/formatting rules per request
    "operator-linebreak": "off",
    "brace-style": "off",
    "block-spacing": "off",
  },
  overrides: [
    {
      files: ["**/*.spec.*"],
      env: {
        mocha: true,
      },
      rules: {},
    },
  ],
  globals: {},
};
