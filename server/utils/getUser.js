const parsePermissions = require("./permissionArrayToObject");

module.exports = async jwt => {
  if (jwt) {
    const payload = await mg.services.jwt.verify(jwt);

    if (payload) {
      const user = await mg.query('user').findOne({
        id: payload.id
      }, ['role.permission']);

      if (user) {
        user.permissions = parsePermissions(
          user._relations.role._relations.permission
        );
  
        return user;
      }
    }
  }

  return null;
};