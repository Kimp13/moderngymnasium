// server dependencies
import dotenv from 'dotenv';
import sirv from 'sirv';
import polka from 'polka';
import cookie from 'cookie';
import bodyParser from 'body-parser';
import compression from 'compression';
import * as sapper from '@sapper/server';

// database
import Knex from 'knex';
import Bookshelf from 'bookshelf';

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
const bookshelf = new Bookshelf(knex);

// working with filesystem
const srcPath = path.join(process.cwd(), 'src');
const modelsPath = path.join(srcPath, 'api');
const configPath = path.join(srcPath, 'config');

// environment
const { PORT, NODE_ENV, API_URL } = process.env;
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

/**
 * Main function of the server.
 * @returns nothing
 */
const main = () => {
  // proceed if connected to database
  knex.raw('select 1 + 1 as testValue').then(() => {
      fs.readdir(modelsPath, (err, files) => {
        if (err) {
          console.log('You have no API. I don\'t want to start the server.');
          return;
        };
  
        mg.paths = new Object();
        mg.models = new Object();
        mg.services = new Object();
        mg.cache = new Object();

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
                  
                  let model = require(modelPath);
          
                  knex.schema.hasTable(model.tableName).then(exists => {
                    if (exists) {
                      mg.models[file] = bookshelf.model(file, {
                        requireFetch: false,
                        ...model
                      });
        
                      if (file.toLowerCase() === 'user') {
                        mg
                          .models[file]
                          .count()
                          .then(count => (mg.cache.usersCount = count));
                      }
                    }
                    
                    resolve();
                  });
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
        ).then(() => {
          polka()
            .use(compression({ threshold: 0 }))
            .use(sirv('static', { dev }))
            .use(bodyParser.json({ extended: true }))
            .use(async (req, res, next) => {
              const start = new Date();
          
              await next();
          
              const ms = Date.now() - start.getTime();
          
              console.log(
                `${start.toLocaleString()} | ${req.method} on ` +
                req.url + ' took ' + ms + ' ms'
              );
            })
            .use(async (req, res, next) => {
              req.cookies = cookie.parse(req.headers.cookie || '');

              const questionMarkIndex = req.url.indexOf('?');

              if (questionMarkIndex === -1) {
                req.path = req.url;
                req.search = '';
              } else {
                req.path = req.url.substring(0, questionMarkIndex);
                req.search = req.url.substring(questionMarkIndex + 1);
              }
      
              if (req.path.substring(0, 4) === '/api') {
                req.path = req.path.substring(4);

                if (req.path.charAt(req.path.length - 1) !== '/') {
                  req.path += '/';
                }

                if (
                  mg.paths.hasOwnProperty(req.method) &&
                  mg.paths[req.method].hasOwnProperty(req.path)
                ) {
                  await mg.paths[req.method][req.path](req, res);
                } else {
                  res.status = 404;
                }
              } else {
                await next();
              }
      
              return;
            })
            .use(async (req, res, next) => {
              let user = await getUser(req.cookies.jwt);
      
              if (user) {
                user = Object.assign({
                  isAuthenticated: true
                }, _.pick(user, ['first_name', 'last_name', 'username', 'permissions']))
              } else {
                user = {
                  isAuthenticated: false
                }
              }
              
              sapper.middleware({
                session: () => {
                  return {
                    apiUrl: API_URL,
                    user
                  };
                }
              })(req, res, next);
            })
            .listen(PORT, err => {
              if (err) console.log('error', err);
            });
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
