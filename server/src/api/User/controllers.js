const bcrypt = require('bcrypt');
const pick = require('lodash/pick');
const parsePermissions = require('../../../utils/permissionArrayToObject');

module.exports = {
  find: async (req, res) => {
    res.end('OK');

    return;
  },

  count: async (req, res) => {
    res.statusCode = 200;
    res.end(JSON.stringify({
      count: mg.cache.usersCount
    }));

    return;
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
        const user = await (new mg.models.User({
          username,
          password: hash,
          role_id: 1
        }).save());

        const jwt = mg.services.jwt.issue({
          id: user.attributes.id
        });

        if (jwt) {
          res.statusCode = 200;
          res.end(JSON.stringify({
            user: Object.assign({
              isAuthenticated: true
            }, pick(user, ['first_name', 'last_name', 'username', 'permissions'])),
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
      password.length > 8 &&
      !/[^0-9a-zA-Z#$*_]/.test(username)
    ) {
      const user = (await mg.models.User.where({
        username
      }).fetch({
        withRelated: ['role.permissions']
      })).toJSON();

      if (
        user &&
        await bcrypt.compare(password, user.password.toString())
      ) {
        const jwt = mg.services.jwt.issue({
          id: user.id
        });

        user.permissions = parsePermissions(user.role.permissions);

        res.statusCode = 200;
        res.end(JSON.stringify({
          jwt,
          data: pick(user, ['first_name', 'last_name', 'username', 'permissions'])
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