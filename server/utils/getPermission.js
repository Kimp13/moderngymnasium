const get = require('lodash/get');

module.exports = (permissions, required) => {
  for (let i = 0; i < required.length; i += 1) {
    if (permissions['*']) {
      return true;
    }

    let i = 0;
    let current = permissions[required[i++]];

    while(
      !(current instanceof Array) &&
      current instanceof Object
    ) {
      current = current[required[i++]];
    }

    return current;
  }

  return false;
};