const parsePermissions = require("./permissionArrayToObject");

module.exports = async jwt => {
  if (jwt) {
    const payload = await mg.services.jwt.verify(jwt);

    if (payload) {
      let user = (await mg.models.User.where('id', payload.id).fetch({
        withRelated: ['role.permissions']
      })).toJSON();

      user.permissions = parsePermissions(user.role.permissions);
      delete user.role;

      return user;
    }
  }

  return null;
};