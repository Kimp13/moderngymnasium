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

  find: async (req, res) => {
    res.send(await mg.services.role.getUsersCreateMap(req.user));
  }
}