const parsePermissions = require("./permissionArrayToObject");

module.exports = async jwt => {
  if (jwt) {
    const payload = await mg.services.jwt.verify(jwt);

    if (payload) {
      const user = await mg.query('user').findOne({
        id: payload.id
      });

      if (user) {
        user.permissions = parsePermissions(
          await mg.knex
            .select('*')
            .from('permissions')
            .innerJoin(
              'permission_role',
              'permission_role.permission_id',
              'permission.id'
            )
            .where('permission_role.role_id', user.role_id)
        );
  
        return user;
      }
    }
  }

  return null;
};