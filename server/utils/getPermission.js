const get = require('lodash/get');

module.exports = (permissions, required) => {
  for (let i = 0; i < required.length; i += 1) {
    if (permissions['*']) {
      return true;
    }

    while (required.length) {
      const value = get(permissions, required);
      
      if (value) {
        return value;
      }

      required.pop();
    }
  }

  return false;
};