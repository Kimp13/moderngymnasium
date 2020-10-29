module.exports = {
  preprocess: require('svelte-preprocess')({
    scss: {
      includePaths: ['./src/theme'],
    },

    postcss: {
      plugins: [require('autoprefixer')],
    },

    babel: {
      presets: [
        [
          '@babel/preset-env',
          {
            loose: true,
            modules: false,
            targets: {
              esmodules: true
            }
          }
        ]
      ]
    }
  }),
};