// server dependencies
import dotenv from 'dotenv';
import express from 'express';
import bodyParser from 'body-parser';
import cookie from 'cookie';
import compression from 'compression';
import * as sapper from '@sapper/server';

// database
import Knex from 'knex';
import addModels, * as db from '../database';

// filesystem
import path from 'path';
import fs from 'fs';

// utilities
import _ from 'lodash';
import getUser from "../utils/getUser";
import getRouteName from '../utils/getRouteName';

dotenv.config();

// bookshelf instance
const knex = new Knex({
  client: process.env.DB_CLIENT || 'mysql',
  connection: {
    host: process.env.DB_HOST || 'localhost',
    user: process.env.DB_USER || 'root',
    password: process.env.DB_PASSWORD || 'root',
    database: process.env.DB_DATABASE || 'main',
    charset: process.env.DB_CHARSET || 'utf8'
  }
});

// working with filesystem
const srcPath = path.join(process.cwd(), 'src');
const modelsPath = path.join(srcPath, 'api');
const configPath = path.join(srcPath, 'config');

// environment
const { PORT, NODE_ENV, API_URL, PATH_TO_FIREBASE_KEY } = process.env;
const dev = NODE_ENV === 'development';

if (!API_URL) {
  API_URL = 'http://localhost:3000';
}

/**
 * Global variable containing server cache, plugins, models, functions
 * @returns JS object
 */
global.mg = new Object();
mg.knex = knex;

// Firebase
mg.firebaseAdmin = require('firebase-admin');
const firebaseServiceAccount = require(process.cwd() + PATH_TO_FIREBASE_KEY);

/**
 * Main function of the server.
 */
const main = () => {
  // proceed if connected to database
  knex.raw('select 1 + 1 as testValue').then(() => {
    fs.readdir(modelsPath, (err, files) => {
      if (err) {
        console.log('You have no API. I don\'t want to start the server.');
        return;
      };

      mg.paths = {};
      mg.models = {};
      mg.services = {};
      mg.cache = {
        usersTokens: {},
        classes: {}
      };

      Promise.all(
        files.map(file => new Promise((resolve, reject) => {
          let currentPath = path.join(modelsPath, file),
            modelPath = path.join(currentPath, 'model.js'),
            servicesPath = path.join(currentPath, 'services.js'),
            routesPath = path.join(currentPath, 'routes.json'),
            controllersPath = path.join(currentPath, 'controllers.js'),
            routeName = getRouteName(file);

          Promise.all([
            new Promise((resolve, reject) => {
              fs.access(modelPath, fs.F_OK, err => {
                if (err) {
                  resolve();
                  return;
                }

                mg.models[file] = require(modelPath);

                if (file.toLowerCase() === 'user') {
                  mg
                    .knex('user')
                    .count('id')
                    .then(count => {
                      mg.cache.usersCount =
                        count[0][Object.keys(count[0])[0]];

                      resolve();
                    });
                } else {
                  resolve();
                }
              });
            }),
            new Promise((resolve, reject) => {
              fs.access(routesPath, fs.F_OK, err => {
                if (err) {
                  resolve();
                  return;
                }

                fs.access(controllersPath, fs.F_OK, err => {
                  let routes = require(routesPath),
                    controllers = require(controllersPath);

                  for (let j = 0; j < routes.length; j += 1) {
                    routes[j].method = routes[j].method.toUpperCase();

                    if (
                      routes[j].path.charAt(
                        routes[j].path.length - 1
                      ) !== '/'
                    ) {
                      routes[j].path += '/';
                    }

                    if (!mg.paths.hasOwnProperty(routes[j].method)) {
                      mg.paths[routes[j].method] = new Object();
                    }

                    mg.paths[routes[j].method][`/${routeName}${routes[j].path}`] = (
                      routes[j].handler === 'default' ?
                        controllers :
                        controllers[routes[j].handler]
                    );

                    resolve();
                  }
                });
              });
            }),
            new Promise((resolve, reject) => {
              fs.access(servicesPath, fs.F_OK, err => {
                if (err) {
                  resolve();
                  return;
                }

                mg.services[_.camelCase(file)] = require(servicesPath);
                resolve();
              });
            })
          ]).then(resolve);
        }))
      )
        .then(addModels(knex, mg.models))
        .then(() => Promise.all([
          new Promise((resolve, reject) => {
            knex
              .select('user.id', 'messaging_token.token')
              .from('user')
              .innerJoin(
                'messaging_token_user',
                'messaging_token_user.user_id',
                'user.id'
              )
              .innerJoin(
                'messaging_token',
                'messaging_token_user.token_id',
                'messaging_token.id'
              )
              .then(tokens => {
                for (let i = 0; i < tokens.length; i += 1) {
                  if (mg.cache.usersTokens.hasOwnProperty(tokens[i].id)) {
                    mg.cache.usersTokens[tokens[i].id].push(tokens[i].token);
                  } else {
                    mg.cache.usersTokens[tokens[i].id] = [tokens[i].token];
                  }
                }

                resolve();
              }, reject);
          }),
          new Promise((resolve, reject) => {
            knex
              .select('*')
              .from('class')
              .then(classes => {
                for (const classEntity of classes) {
                  if (!mg.cache.classes.hasOwnProperty(classEntity.grade)) {
                    mg.cache.classes[classEntity.grade] = new Object();
                  }

                  mg.cache.classes[classEntity.grade][classEntity.letter] =
                    classEntity.id;
                }

                resolve();
              }, reject);
          })
        ]))
        .then(() => {
          // Initialize Firebase
          mg.firebaseAdmin.initializeApp({
            credential: mg
              .firebaseAdmin
              .credential
              .cert(firebaseServiceAccount),
            databaseURL: 'https://modern-gymnasium.firebaseio.com'
          });

          // Set Query function
          mg.query = db.query;
          mg.queryAll = db.queryAll;

          const app = express();

          app
            .use(bodyParser.json({ extended: true }))
            .use(async (req, res, next) => {
              const start = new Date();

              await next();

              const ms = Date.now() - start.getTime();

              console.log(
                `${start.toLocaleString()} | ${res.statusCode} ` +
                `${req.method} on ${req.originalUrl} took ${ms} ms`
              );
            })
            .use('/api', async (req, res, next) => {
              req.cookies = cookie.parse(req.headers.cookie || '');
              req.search = req.url.substring(req.path.length + 1);

              const path = (
                req.path[req.path.length - 1] === '/' ?
                  req.path :
                  req.path + '/'
              );

              if (
                mg.paths.hasOwnProperty(req.method) &&
                mg.paths[req.method].hasOwnProperty(path)
              ) {
                try {
                  await mg.paths[req.method][path](req, res);
                } catch (e) {
                  console.log(e);

                  res.statusCode = 500;
                  res.end('{}');
                }
              } else {
                res.statusCode = 404;
                res.end('{}');
              }

              return;
            })
            .use(compression({ threshold: 0 }))
            .use(express.static('static'))
            .use(async (req, res, next) => {
              req.cookies = cookie.parse(req.headers.cookie || '');

              req.user = await getUser(req.cookies.jwt);

              sapper.middleware({
                session: () => {
                  return {
                    apiUrl: API_URL,
                    user: (
                      req.user ?
                        Object.assign({
                          isAuthenticated: true
                        }, _.pick(req.user, [
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
              })(req, res, next);
            });

          app.listen(PORT, err => {
            if (err) console.log('error', err);
          });
        }, e => {
          console.log(e);
        });
    });
  });
};

fs.access(configPath, fs.F_OK, err => {
  if (err) main();

  const bootstrapPath = path.join(configPath, 'functions', 'bootstrap.js');
  const commonEnvironmentPath = path.join(configPath, 'environments', 'common.js');
  const specialEnvironmentPath = (
    process.env.NODE_ENV === 'production' ?
      path.join(configPath, 'environments', 'production.js') :
      path.join(configPath, 'environments', 'development.js')
  );

  fs.access(bootstrapPath, fs.F_OK, err => {
    if (err) main();

    require(bootstrapPath)().then(main);
  });

  fs.access(commonEnvironmentPath, fs.F_OK, err => {
    if (err) return;

    let commonConfig = require(commonEnvironmentPath);

    if (mg.config) {
      mg.config = _.defaults(mg.config, commonConfig);
    } else {
      mg.config = commonConfig;
    }
  });

  fs.access(specialEnvironmentPath, fs.F_OK, err => {
    if (err) return;

    let specialConfig = require(specialEnvironmentPath);

    if (mg.config) {
      mg.config = _.defaults(specialConfig, mg.config);
    } else {
      mg.config = specialConfig;
    }
  });
});
