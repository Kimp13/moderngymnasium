const jsonify = require("../../../utils/searchToJson");
const getUser = require("../../../utils/getUser");

module.exports = {
  find: async (req, res) => {
    let { jwt, limit, offset } = jsonify(req.search);
    let user = await getUser(jwt);

    if (!limit) {
      limit = 10;
    }

    if (!offset) {
      offset = 0;
    }

    if (user) {
      user = (await mg.knex('user').where({
        id: user.id
      }))[0];

      if (user) {
        const announcements = await mg.knex('announcement')
          .innerJoin(
            'announcement_class_role',
            'announcement_class_role.announcement_id',
            'announcement.id'
          )
          .where(
            'announcement_class_role.class_id',
            user.class_id
          )
          .andWhere(
            'announcement_class_role.role_id',
            user.role_id
          )
          .offset(offset)
          .limit(limit);

        res.statusCode = 200;
        res.end(JSON.stringify(announcements));
        return;
      }

      res.statusCode = 401;
      res.end('{}');
      return;
    }

    res.statusCode = 400;
    res.end('{}');
    return;
  },

  count: async(req, res) => {
    const { jwt } = jsonify(req.search);
    let user = {
      id: 1
    }//await getUser(search.jwt);

    if (user) {
      user = (await mg.knex('user').where({
        id: user.id
      }))[0];

      if (user) {
        const count = (await mg.knex('announcement')
          .count('*')
          .innerJoin(
            'announcement_class_role',
            'announcement_class_role.announcement_id',
            'announcement.id'
          )
          .where(
            'announcement_class_role.class_id',
            user.class_id
          )
          .andWhere(
            'announcement_class_role.role_id',
            user.role_id
          ))[0]['count(*)'];

        

        res.statusCode = 200;
        res.end(JSON.stringify({
          count
        }));
        return;
      }

      res.statusCode = 401;
      res.end('{}');
      return;
    }

    res.statusCode = 400;
    res.end('{}');
    return;
  }
};