const _ = require('lodash');

module.exports = permissionsArray => {
  let permissionObject = new Object();

  for (let permission of permissionsArray) {
    if (permission.operation) {
        if (permission.role_id) {
          if (_.has(
            permissionObject,
            [permission.type, permission.operation]
          )) {
            permissionObject[type][operation].push(permission.role_id);
          } else {
            _.set(permissionObject, [
              permission.type,
              permission.operation
            ], [permission.role_id]);
          }
        } else {
          _.set(permissionObject, [
            permission.type,
            permission.operation
          ], true);
        }
    } else {
      _.set(permissionObject, permission.type, true);
    }
  }

  return permissionObject;
};