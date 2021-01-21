import getPermission from "getPermission";

export default {
  findOne: async (req, res) => {
    if (Array.isArray(req.query.id)) {
      for (let i = 0; i < req.query.id.length; i += 1) {
        req.query.id[i] = parseInt(req.query.id[i], 10);

        if (isNaN(req.query.id[i])) {
          res.throw(400);
          return;
        }
      }

      res.send(await mg.query('role').find({ id_in: req.query.id }));
    } else {
      const id = parseInt(req.query.id, 10);

      if (!isNaN(id)) {
        const role = await mg.query('role').findOne({ id });

        res.send(role);
        return;
      }

      req.throw(400);
    }
  },

  getMap: async (req, res) => {
    res.send(await mg.services.role.getUsersCreateMap(req.user));
  },

  all: async (req, res) => {
    const permission = getPermission(
      req.user.permissions,
      ['announcement', 'create']
    );

    if (permission === true) {
      res.send(mg.cache.roles.map(role => ({
        id: role.id,
        type: role.type,
        name: role.name
      })));
      return;
    }

    res.throw(403);
  }
}