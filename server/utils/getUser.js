const set = require('lodash/set');

module.exports = async jwt => {
  if (jwt) {
    const payload = await mg.services.jwt.verify(jwt);

    if (payload) {
      let user = (await mg.models.User.where('id', payload.id).fetch({
        withRelated: ['role.permissions']
      })).toJSON();

      let permissions = new Object();

      for (let permission of user.role.permissions) {
        set(permissions, permission.name.split('_'), true);
      }

      delete user.role;
      user.permissions = permissions;

      return user;
    }
  }

  return null;
};