const jsonify = require("../../../utils/searchToJson");

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
  }
}