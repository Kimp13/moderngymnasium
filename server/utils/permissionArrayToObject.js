const has = require('lodash/has');
const set = require('lodash/set');

module.exports = (permissionsArray = []) => {
  let permissionObject = new Object();

  for (const permission of permissionsArray) {
    if (permission.operation) {
      if (permission.target) {
        if (has(
          permissionObject,
          [permission.type, permission.operation]
        )) {
          permissionObject[type][operation].push(permission.target);
        } else {
          set(permissionObject, [
            permission.type,
            permission.operation
          ], [permission.target]);
        }
      } else {
        set(permissionObject, [
          permission.type,
          permission.operation
        ], true);
      }
    } if (permission.type === '*') {
      return true;
    } else {
      set(permissionObject, permission.type, true);
    }
  }
  
  return permissionObject;
};