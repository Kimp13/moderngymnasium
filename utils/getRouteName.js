module.exports = function(name) {
  name = name.replace(/\s+/g, '').toLowerCase();

  if (name.charAt(name.length - 1) !== 's') {
    name += 's';
  }

  return name;
};