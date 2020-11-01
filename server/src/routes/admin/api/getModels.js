import defaults from 'lodash/defaults';
import omit from 'lodash/omit';

import getPermission from "getPermission";

export async function get(req, res, next) {
  const permission = getPermission(
    req.user.permissions,
    ['admin', 'insert']
  );

  if (permission === true) {
    const result = [];
    const models = mg.queryAll('unspecified');
    const keys = Object.keys(models);

    for (const key of keys) {
      result.push(omit(models[key].specs, ['relations']));
    }

    res.statusCode = 200;
    res.end(JSON.stringify(
      result
    ));
  } else {
    res.statusCode = 404;
    res.end('{}');
  }
};