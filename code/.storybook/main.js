module.exports = {
  stories: ["../stories-js/*.stories.@(js|jsx|ts|tsx)"],

  framework: {
    name: "@storybook/react-webpack5",
    options: {}
  },

  features: {
  },

  staticDirs: ['../resources/public'],

  webpackFinal: async (config) => {
    config.resolve.fallback = {
      "request": false,
      "http": false,
      "https": false
    };
    return config;
  },

  addons: [
    "@storybook/addon-webpack5-compiler-babel",
    "@storybook/addon-essentials",
    "@storybook/addon-interactions",
    "@storybook/test-runner"
  ],

  docs: {
    autodocs: true
  },

  typescript: {
    reactDocgen: "react-docgen-typescript"
  }
};
