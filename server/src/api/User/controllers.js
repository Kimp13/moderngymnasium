import bcrypt from 'bcrypt';
import pick from 'lodash/pick';
import get from 'lodash/get';
import parsePermissions from 'permissionArrayToObject';

module.exports = {
  find: async (req, res) => {
    const id = parseInt(req.query.id, 10);

    if (!isNaN(id)) {
      const user = await mg.query('user').findOne({
        id
      }, [], [
        'id',
        'firstName',
        'lastName',
        'roleId',
        'classId'
      ]);

      res.send(user);
      return;
    }

    res.throw(400);
    return;
  },

  count: async (_, res) => {
    res.send({
      count: mg.cache.usersCount
    });
  },

  me: async (req, res) => {
    res.send({
      jwt: "",
      data: {
        firstName: req.user.firstName,
        lastName: req.user.lastName,
        username: req.user.username,
        id: req.user.id,
        roleId: req.user.roleId,
        classId: req.user.classId,
        permissions: req.user.permissions
      }
    });
  },

  signUp: async (req, res) => {
    const { username, password } = req.body;

    if (
      !password ||
      password.length < 8 ||
      /[^0-9a-zA-Z#$*_]/.test(username)
    ) {
      res.statusCode = 400;
      res.end('{}');

      return;
    }

    try {
      if (mg.cache.usersCount === 0) {
        const hash = await bcrypt.hash(password, 10);
        const user = await mg.knex
          .select('*')
          .from('user')
          .where('id', (
            await mg.knex('user')
              .insert({
                username,
                password: hash,
                role_id: 1
              })
          )[0]);

        const jwt = mg.services.jwt.issue({
          id: user.attributes.id
        });

        if (jwt) {
          res.statusCode = 200;
          res.end(JSON.stringify({
            user: Object.assign({
              isAuthenticated: true
            }, pick(user, [
              'first_name',
              'last_name',
              'username',
              'permissions',
              'role_id',
              'class_id'
            ])),
            jwt
          }));

          mg.cache.usersCount += 1;

          return;
        }

        console.log(`Jwt test failed! It's ${jwt}`);

        res.statusCode = 500;
        res.end('{}');
      } else {

      }
    } catch (e) {
      console.log(e);

      res.statusCode = 500;
      res.end('{}');

      return;
    }
  },

  signIn: async (req, res) => {
    const { username, password } = req.body;

    if (
      password &&
      username &&
      password.length >= 8 &&
      !/[^0-9a-zA-Z#$*_]/.test(username)
    ) {
      const user = await mg.query('user').findOne({
        username
      }, ['role.permission']);

      if (
        user &&
        await bcrypt.compare(password, user.password.toString())
      ) {
        const jwt = mg.services.jwt.issue({
          id: user.id
        });

        user.permissions = parsePermissions(get(
          user,
          [
            '_relations',
            'role',
            '_relations',
            'permission'
          ]
        ));

        res.statusCode = 200;
        res.end(JSON.stringify({
          jwt,
          data: pick(user, [
            'firstName',
            'lastName',
            'username',
            'permissions',
            'roleId',
            'classId'
          ])
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