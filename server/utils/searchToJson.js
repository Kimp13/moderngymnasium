module.exports = url => {
  if (typeof url === 'string') {
    let result = new Object();

    url = decodeURI(url);

    for (let argument of url.split('&')) {
      const parts = argument.split('=');

      result[parts[0]] = parts[1];
    }

    return result;
  }
  
  return new Object();
}