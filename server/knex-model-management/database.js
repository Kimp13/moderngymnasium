import fs from 'fs';
import path from 'path';
import set from 'lodash/set';
import get from 'lodash/get';
import defaults from 'lodash/defaults';

const readdirAsync = path => new Promise((resolve, reject) => {
  fs.readdir(path, (err, files) => {
    if (err) {
      reject(err);
    } else {
      resolve(files);
    }
  });
});

const _models = {};

export const parseArgs = (query, args = {}, property = 'where') => {
  let result = query;
  const keys = Object.keys(args);

  for (let i = 0; i < keys.length; i += 1) {
    const key = keys[i];
    const method = i ? property : 'where';

    if (key === '_or') {
      result = result[method](builder => {
        parseArgs(
          builder,
          args._or,
          'orWhere'
        )
      });
    } else if (key === '_and') {
      result = result[method](builder => {
        parseArgs(
          builder,
          args._and,
          'where'
        )
      });
    } else {
      const lastLoDash = key.lastIndexOf('_'); // Sounds like a film name :)
      let column, end;

      if (lastLoDash === -1) {
        column = key;
        end = '';
      } else {
        column = key.substring(0, lastLoDash); // No one likes to see
        end = key.substring(lastLoDash + 1); // Last Lo Dash :(
      }

      if (end === 'ne') {
        result = result[method](column, '<>', args[key]);
      } else if (end === 'lt') {
        result = result[method](column, '<', args[key]);
      } else if (end === 'gt') {
        result = result[method](column, '>', args[key]);
      } else if (end === 'lte') {
        result = result[method](column, '<=', args[key]);
      } else if (end === 'gte') {
        result = result[method](column, '>=', args[key]);
      } else if (end === 'in') {
        result = result[method + 'In'](column, args[key]);
      } else if (end === 'nin') {
        result = result[method + 'NotIn'](column, args[key]);
      } else if (end === 'contains') {
        result = result[method](column, 'like', `%${args[key]}%`);
      } else if (end === 'ncontains') {
        result = result[method](column, 'not like', `%${args[key]}%`);
      } else if (end === 'containss') {
        result = result[method + 'Raw'](
          `?? like binary '%${args[key]}%'`,
          column
        );
      } else if (end === 'ncontainss') {
        result = result[method + 'Raw'](
          `?? not like binary '%${args[key]}%'`,
          column
        );
      } else {
        result = result[method](column, '=', args[key]);
      }
    }
  }

  return result;
};

export class KnexManageModel {
  constructor(knex, model) {
    this.model = model;
    this.knex = knex;
  }

  _findBase(args = {}) {
    const [orderColumn, orderType] = (
      args.hasOwnProperty('_sort') ?
        args._sort.split(':') :
        [false]
    );

    const limit = (
      args.hasOwnProperty('_limit') ?
        args._limit :
        100
    );

    const offset = (
      args.hasOwnProperty('_skip') ?
        args._skip :
        0
    );

    delete args._sort;
    delete args._limit;
    delete args._skip;

    let query = parseArgs(
      this.knex
        .select('*')
        .from(this.model.tableName),
      args
    );

    if (orderColumn !== false) {
      query = query.orderBy(orderColumn, orderType);
    }

    query = query.limit(limit);
    query = query.offset(offset);

    return query;
  }

  find(args = {}) {
    return this._findBase(args);
  }

  findOne(args = {}) {
    args._limit = 1;

    return this._findBase(args).then(result => result[0]);
  }

  delete(args = {}) {
    return parseArgs(this.knex(model.tableName), args)
      .del();
  }

  update(where = {}, set = {}) {
    return parseArgs(this.knex(model.tableName), where)
      .update(set);
  }
};

export const initializeColumn = (table, name, column) => {
  switch (column.type) {
    case 'text':
      return table.text(name, column.textType);

    case 'string':
    case 'binary':
      return table[column.type](name, column.length);

    case 'float':
    case 'decimal':
      return table[column.type](name, column.precision, column.scale);

    case 'datetime':
    case 'timestamp':
      return table[column.type](name, column.options);

    case 'time':
      return table.time(name, column.precision);

    case 'enum':
    case 'enu':
      return table.enu(name, column.values, column.options);

    default:
      return table[column.type](name);
  }
};

export const heavyInit = (table, name, column, uniques) => {
  let dbColumn = initializeColumn(table, name, column);

  if (column.unique) {
    uniques.push(key);
  }

  if (column.notNull) {
    dbColumn = dbColumn.notNullable();
  }

  if (column.default) {
    dbColumn = dbColumn.defaultTo(column.default);
  }

  return dbColumn;
};

export const relationColumn = (trx, relation) =>
  new Promise((resolve, reject) => {
    trx.schema.hasColumn(
      relation.from.tableName,
      relation.fromColumn
    ).then(exists => {
      if (exists) {
        trx.schema.alterTable(relation.from.tableName, table => {
          if (relation.notNull) {
            table
              .integer(relation.fromColumn)
              .unsigned()
              .notNull()
              .alter();
          } else {
            table
              .integer(relation.fromColumn)
              .unsigned()
              .alter();
          }
        }).then(resolve);
      } else {
        trx.schema.table(relation.from.tableName, table => {
          if (relation.notNull) {
            table
              .integer(relation.fromColumn)
              .unsigned()
              .notNull();
          } else {
            table
              .integer(relation.fromColumn)
              .unsigned();
          }

          table.foreign(relation.fromColumn).references(
            `${relation.to.tableName}.${relation.toColumn}`
          );
        }).then(resolve);
      }
    });
  });

export const query = (name, origin = 'unspecified') => {
  return get(_models, [origin, name], null);
};

export const addModel = (key, model) => _models[key] = model;

export default function (knex, models) {
  const relations = [];
  const stdModels = {};
  const modelsPath = path.join(
    process.cwd(),
    'knex-model-management',
    'models'
  );

  return knex.transaction(trx => {
    readdirAsync(modelsPath)
      .then(files => {
        for (const file of files) {
          const model = require(path.join(modelsPath, file));
          model.origin = 'kmm_admin';

          stdModels[`${model.origin}_${model.tableName}`] = model;
        }

        models = defaults(stdModels, models);
      })
      .then(() =>
        Promise.all(
          Object.keys(models).map(key => new Promise((resolve, reject) => {
            const res = () => {
              set(
                _models,
                [model.origin, model.suffixTableName],
                new KnexManageModel(knex, model)
              );

              resolve();
            };

            const model = models[key];

            if (!model.hasOwnProperty('origin')) {
              model.origin = 'unspecified';
            }

            if (model.hasOwnProperty('relations')) {
              for (const relation of model.relations) {
                if (typeof relation.with === 'string') {
                  relation.with = [relation.with, 'unspecified'];
                }

                relation.from = model;
                relations.push(relation);
              }
            }

            model.suffixTableName = model.tableName;

            if (
              model.origin &&
              model.origin !== 'unspecified'
            ) {
              model.tableName = `${model.origin}_${model.tableName}`;
            }

            trx.schema.hasTable(model.tableName).then(exists => {
              if (exists) {
                trx.raw(`describe ${model.tableName}`)
                  .then(description => {
                    description = description[0];

                    trx.schema.alterTable(model.tableName, table => {

                      const hasRaw = {};
                      const primaryKeys = [];
                      const uniques = [];

                      for (const rawColumn of description) {
                        if (rawColumn.Field !== 'id') {
                          const initColumn = () => {
                            if (dbColumn === undefined) {
                              dbColumn =
                                initializeColumn(
                                  table,
                                  rawColumn.Field,
                                  column
                                );
                            }
                          };

                          const column = model.columns[rawColumn.Field];
                          let dbColumn;
                          let type;

                          if (column) {
                            hasRaw[rawColumn.Field] = true;

                            if (
                              rawColumn.Type.substring(0, 7) === 'varchar'
                            ) {
                              type = 'string';
                            } else if (
                              rawColumn.Type.substring(0, 6) === 'binary'
                            ) {
                              type = 'binary';
                            } else {
                              type = rawColumn.Type;
                            }

                            initColumn();

                            if (column.notNull) {
                              dbColumn = dbColumn.notNullable();
                            } else {
                              dbColumn = dbColumn.nullable();
                            }

                            if (rawColumn.Key === 'PRI') {
                              if (!(column.primary || column.increments)) {
                                table.dropPrimary();
                              }
                            } else if (rawColumn.Key === 'UNI') {
                              if (!column.unique) {
                                table.dropUnique(rawColumn.Field);
                              }
                            } else if (rawColumn.Key === 'MUL') {
                              if (!column.hasOwnProperty('foreign')) {
                                table.dropForeign(rawColumn.Field);
                              }
                            } else if (column.primary) {
                              primarykeys.push(rawColumn.Field);
                            } else if (column.increments) {
                              dbColumn = dbColumn.increments();
                            } else if (column.unique) {
                              uniques.push(rawColumn.Field);
                            }

                            if (column.default) {
                              dbColumn = dbColumn.defaultTo(column.default);
                            }

                            dbColumn.alter();
                          }
                        }
                      }

                      for (const key of Object.keys(model.columns)) {
                        if (!hasRaw.hasOwnProperty(key)) {
                          const dbColumn = heavyInit(
                            table,
                            key,
                            model.columns[key],
                            uniques
                          );

                          if (model.columns[key].increments) {
                            dbColumn.increments();
                          } else if (model.columns[key].primary) {
                            primaryKeys.push(key);
                          }
                        }
                      }

                      if (primaryKeys.length > 0) {
                        table.primary(primaryKeys);
                      }

                      if (uniques.length > 0) {
                        table.unique(uniques);
                      }
                    }).then(res);
                  });
              } else {
                trx.schema.createTable(model.tableName, table => {
                  const uniques = [];
                  const primaryKeys = [];
                  const columnsKeys = Object.keys(model.columns);
                  let hasInlinePrimary = false;

                  if (model.hasOwnProperty('table')) {
                    if (model.table.timestamps) {
                      table.timestamps();
                    }

                    if (model.table.comment) {
                      table.comment(model.table.comment);
                    }

                    if (model.table.engine) {
                      table.engine(model.table.engine);
                    }

                    if (model.table.charset) {
                      table.charset(model.table.charset);
                    }

                    if (model.table.collate) {
                      table.collate(model.table.collate);
                    }
                  }

                  for (const key of columnsKeys) {
                    const column = model.columns[key];
                    let dbColumn = heavyInit(
                      table,
                      key,
                      column,
                      uniques
                    );

                    if (column.increments) {
                      dbColumn = dbColumn.increments();
                      hasInlinePrimary = true;
                    } else if (column.primary) {
                      primaryKeys.push(key);
                    }
                  }

                  if (uniques.length > 0) {
                    table.unique(uniques);
                  }

                  if (!hasInlinePrimary) {
                    if (primaryKeys.length > 0) {
                      table.primary(primaryKeys);
                    } else {
                      table.increments('id');
                    }
                  }
                }).then(res);
              }
            })
          }))
        ))
      .then(() => Promise.all(
        relations.map(relation => new Promise((resolve, reject) => {
          const [typeFrom, typeTo] = relation.type.split(':');
          relation.to = query(...relation.with).model;

          if (typeFrom === 'many') {
            if (typeTo === 'many') {
              relation.junctionTableName = (
                relation
                  .from
                  .tableName
                  .localeCompare(relation.to.tableName) < 0 ?
                  `${relation.from.tableName}_${relation.to.tableName}` :
                  `${relation.to.tableName}_${relation.from.tableName}`
              );

              relation.fromColumn = 'id';
              relation.toColumn = 'id';
              relation.junctionFromColumn =
                `${relation.from.tableName}_id`;
              relation.junctionToColumn =
                `${relation.to.tableName}_id`;

              trx.schema.hasTable(relation.junctionTableName)
                .then(exists => {
                  if (exists) {
                    trx.schema.alterTable(
                      relation.junctionTableName,
                      table => {
                        table.integer(
                          relation.junctionFromColumn
                        ).notNull().alter();

                        table.integer(
                          relation.junctionToColumn
                        ).notNull().alter();
                      }
                    ).then(resolve);
                  } else {
                    trx.schema.createTable(
                      relation.junctionTableName,
                      table => {

                        table.integer(
                          relation.junctionFromColumn
                        ).notNull();
                        table.foreign(
                          relation.junctionFromColumn
                        ).references(
                          `${relation.from.tableName}.${relation.fromColumn}`
                        );

                        table.integer(
                          relation.junctionToColumn
                        ).notNull();
                        table.foreign(
                          relation.junctionToColumn
                        ).references(
                          `${relation.to.tableName}.${relation.toColumn}`
                        );

                        table.primary([
                          relation.junctionFromColumn,
                          relation.junctionToColumn
                        ]);
                      }
                    ).then(resolve);
                  }
                });
            } else {
              relation.fromColumn = `${relation.to.tableName}_id`;
              relation.toColumn = 'id';

              relationColumn(trx, relation).then(resolve);
            }
          } else {
            if (typeTo === 'many') {
              relation.fromColumn = 'id';
              relation.toColumn = `${relation.from.tableName}_id`;

              resolve();
            } else {
              relation.fromColumn = `${relation.to.tableName}_id`;
              relation.toColumn = 'id';

              relationColumn(trx, relation).then(resolve);
            }
          }
        }))
      ))
      .then(() => {
        trx.commit();
      })
      .catch(e => {
        console.log(e);
        trx.rollback();
      });
  })
};
