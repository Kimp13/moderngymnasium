import { getCookie } from "./cookies";

const createUrl = (path, query, auth) => {
  let keys,
    queryString = '';

  if (query) {
    keys = Object.keys(query);

    if (keys.length > 0) {
      queryString = `?${keys[0]}=${query[keys[0]]}`;
    }
  } else {
    keys = new Array();
  }

  for (let i = 1; i < keys.length; i += 1) {
    queryString += `&${keys[i]}=${query[keys[i]]}`;
  }

  return encodeURI(path + queryString);
};

export const getApiResponse = async (path, query, auth) => {
  if (auth === true) {
    auth = getCookie('jwt');
  }

  const url = createUrl(path, query, auth);
  const response = await fetch(url, {
    method: 'GET',
    headers: {
      'Custom-Authorization': auth || ''
    }
  }); 

  if (response.ok) {
    return await response.json();
  } else {
    throw response;
  }
};

export const getPreloadApiResponse = async (path, query, sapperInstance) => {
  const url = createUrl(path, query);
  const response = await (
    await sapperInstance.fetch(url, {
      credentials: 'include'
    })
  ).json()

  return response.hasOwnProperty('response') ?
    response.response :
    response;
};

export const postApi = async (path, query, auth) => {
  if (auth === true) {
    auth = getCookie('jwt');
  }

  const response = await fetch(path, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authentication': auth || ''
    },
    body: JSON.stringify(query)
  });

  if (response.ok) {
    return await response.json();
  } else {
    throw response;
  }
};