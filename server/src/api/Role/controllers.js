const jsonify = require("../../../utils/searchToJson");
const getPermission = require("../../../utils/getPermission");

module.exports = {
  find: async (req, res) => {
    const id = Number(jsonify(req.search).id);

    if (!isNaN(id)) {
      const role = await mg.knex
        .select('*')
        .from('role')
        .where('id', id);

      res.statusCode = 200;
      res.end(JSON.stringify(role[0]));
      return;
    }

    req.statusCode = 400;
    res.end('{}');
    return;
  },

  findAll: async (req, res) => {
    const user = getUser(req.headers.authentication);

    if (user) {
      const permission = getPermission(user.permissions, ['*']);

      if (permission === true) {
        res.statusCode = 200;
        res.end(JSON.stringify(await mg.query('role').find()));
        return
      }
    }

    res.statusCode = 401;
    res.end('{}');
  }
}