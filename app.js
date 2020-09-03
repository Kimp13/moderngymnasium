// configuring .env
require('dotenv').config();

// all dependencies
const Koa = require('koa');
const app = new Koa();
const knex = require('knex')({
  client: process.env.DB_CLIENT,
  connection: {
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_DATABASE,
    charset: process.env.DB_CHARSET
  }
});
const bookshelf = require('bookshelf')(knex);
const path = require('path');
const fs = require('fs');

// utilities
const getTableName = require('./utils/getTableName');

// working with filesystem
const modelsPath = path.join(__dirname, 'api');

// proceed if connected to database
knex.raw('select 1 + 1 as testValue').then(() => {
  fs.readdir(modelsPath, function(err, files) {
    for (let i = 0; i < files.length; i += 1) {
      let currentPath = path.join(modelsPath, files[i]);

      console.log(`Reading ${currentPath} folder.`);

      knex.schema.hasTable(getTableName(files[i])).then(exists => {
        console.log(exists);
      });
      
      console.log(require(path.join(currentPath, 'routes.json')));
      console.log(require(path.join(currentPath, 'controllers')));
    }
  });

  // logging out info about all requests
  app.use((ctx, next) => {
    const start = new Date();
    next().then(() => {
      const ms = Date.now() - start.getTime();
      console.log(
        `${start.toISOString()} | ${ctx.method} request on ${ctx.url}` +
        'took ' + ms + ' milliseconds'
      );
    });
  });

  // main server function
  app.use(ctx => {
    ctx.body = {
      message: 'Hello, World!'
    };
  });

  // setting listening port
  app.listen(1056);
});
