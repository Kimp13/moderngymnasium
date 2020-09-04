// configuring .env
require('dotenv').config();

// all dependencies
const Koa = require('koa');
const app = new Koa();
const knex = require('knex')({
  client: process.env.DB_CLIENT || 'mysql',
  connection: {
    host: process.env.DB_HOST || 'localhost',
    user: process.env.DB_USER || 'root',
    password: process.env.DB_PASSWORD || 'root',
    database: process.env.DB_DATABASE || 'main',
    charset: process.env.DB_CHARSET || 'utf8'
  }
});
const bookshelf = require('bookshelf')(knex);
const path = require('path');
const fs = require('fs');

// utilities
const getRouteName = require('./utils/getRouteName');
const getTableName = require('./utils/getTableName');

// working with filesystem
const modelsPath = path.join(__dirname, 'api');

/**
 * Global variable containing server cache, plugins, models, functions
 * @returns JS object
 */
const mg = new Object();
global.mg = mg;

// proceed if connected to database
knex.raw('select 1 + 1 as testValue').then(() => {
  // gathering models
  fs.readdir(modelsPath, (err, files) => {
    mg.paths = new Object();
    mg.models = new Object();
    mg.services = new Object();

    for (let i = 0; i < files.length; i += 1) {
      let currentPath = path.join(modelsPath, files[i]),
          modelPath = path.join(currentPath, 'model.js'),
          servicesPath = path.join(currentPath, 'services.js'),
          routesPath = path.join(currentPath, 'routes.json'),
          controllersPath = path.join(currentPath, 'controllers.js'),
          routeName = getRouteName(files[i]),
          tableName = getTableName(files[i]);

      fs.access(modelPath, fs.F_OK, err => {
        if (err) return;

        knex.schema.hasTable(tableName).then(exists => {
          if (exists) {
            mg.models[files[i]] = bookshelf.model(files[i], {
              tableName,
              ...require(modelPath)
            });
          }
        });
      });

      fs.access(servicesPath, fs.F_OK, err => {
        if (err) return;

        mg.services[files[i]] = require(servicesPath);
      });

      fs.access(routesPath, fs.F_OK, err => {
        if (err) return;

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
    
            mg.paths[routes[j].method][`/${routeName}${routes[j].path}`] = 
              controllers[routes[j].handler];
          }
        });
      });
    }
  });

  // logging out info about all requests
  app.use((ctx, next) => {
    const start = new Date();

    next().then(() => {
      const ms = Date.now() - start.getTime();

      console.log(
        `${start.toLocaleString()} | ${ctx.status} ${ctx.method} request on ` +
        ctx.url + ' took ' + ms + ' milliseconds'
      );
    });
  });

  // main server function
  app.use(async ctx => {
    let location = /((\/\w*)+)\??(.*)/.exec(ctx.url);

    ctx.location = {
      path: location[1] + (location[2].length === 1 ? '' : '/'),
      search: location[3]
    };

    if (
      mg.paths.hasOwnProperty(ctx.method) &&
      mg.paths[ctx.method].hasOwnProperty(ctx.url)
    ) {
      await mg.paths[ctx.method][ctx.url](ctx);
    } else {
      ctx.status = 404;
    }

    return;
  });

  // setting listening port
  app.listen(1056);
});
