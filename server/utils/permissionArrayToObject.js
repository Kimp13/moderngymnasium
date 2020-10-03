const has = require('lodash/has');
const set = require('lodash/set');

module.exports = permissionsArray => {
  let permissionObject = new Object();

  for (let permission of permissionsArray) {
    if (permission.operation) {
      if (permission.role_id) {
        if (has(
          permissionObject,
          [permission.type, permission.operation]
        )) {
          permissionObject[type][operation].push(permission.role_id);
        } else {
          set(permissionObject, [
            permission.type,
            permission.operation
          ], [permission.role_id]);
        }
      } else {
        set(permissionObject, permission.type, permission.operation);
      }
    } else {
      set(permissionObject, permission.type, true);
    }
  }

  return permissionObject;
};