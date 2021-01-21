module.exports = (permissions, required) => {
  let i = 0;
  let current = permissions;

  while (required[i] && Boolean(current) && current.constructor === Object) {
    current = current[required[i++]];
  }

  return current;
};