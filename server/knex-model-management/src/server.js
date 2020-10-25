// Server itself
import * as sapper from '@sapper/server';
import express from 'express';

// dependencies
import cookie from 'cookie';
import bodyParser from 'body-parser';
import _ from 'lodash';
import path from 'path';

// Utilities
import getUser from '../utils/getUser';

function _throw (code = 404) {
  this.statusCode = code;
  this.end('{}');
};

function _send (payload = {}, code = 200) {
  this.statusCode = code;
  this.end(JSON.stringify({
    response: payload
  }));
};

const app = (subpath) => {
  const subapp = express();

  subapp
    .use(express.static(
      path.join(process.cwd(), 'knex-model-management', 'static')
    ))
    .use(bodyParser.json({ extended: true }))
    .use(async (req, res) => {
      res.throw = _throw;
      res.send = _send;
      req.cookies = cookie.parse(req.headers.cookie || '');

      const user = await getUser(req.cookies.kmm_admin_jwt);

      sapper.middleware({
        session: () => {
          return {
            apiUrl: subpath,
            user: (
              user ?
                Object.assign({
                  isAuthenticated: true
                }, _.pick(user, [
                  'first_name',
                  'last_name',
                  'username',
                  'permissions'
                ])) :
                {
                  isAuthenticated: false
                }
            )
          };
        }
      })(req, res, () => null);
    });

  return subapp;
}

export default app;
