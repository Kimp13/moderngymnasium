'use strict';

import set from 'lodash/set';
import get from 'lodash/get';
import bcrypt from 'bcrypt';

const UTILITY_DATABASE_NAME = 'tables';
const _models = {};
let hasInitialized = false;

class KnexManageModel {
  constructor(knex, model) {
    this.specs = model;
    this.knex = knex;
  }

  async _findRelated(entity, related, model = this) {
    const dotIndex = related.indexOf('.');
    const relatedModelName = (
      dotIndex === -1 ?
        related :
        related.substring(0, dotIndex)
    );
    const next = (
      dotIndex === -1 ?
        false :
        related.substring(dotIndex + 1)
    );

    if (model.specs.hasOwnProperty('relations')) {
      const relation = model.specs.relations[
        `${relatedModelName},${model.specs.origin}`
      ];

      if (relation) {
        const type = relation.type.split(':');
        const relatedModel = query(...relation.with);
        const relatedModelRelation = (
          relatedModel.specs.hasOwnProperty('relations') ?
            relatedModel.specs.relations[
            model.specs.tableName + ',' + model.specs.origin
            ] :
            {
              column: 'id'
            }
        ) || {
          column: 'id'
        };

        if (type[1] === 'many') {
          let relatedEntities;

          if (!relatedModelRelation.hasOwnProperty('junctionColumn')) {
            relatedModelRelation.junctionColumn =
              relatedModelName + '_id';
          }

          if (type[0] === 'many') {
            relatedEntities = await this.knex
              .select(`${relatedModelName}.*`)
              .from(relatedModelName)
              .innerJoin(
                relation.junctionTableName,
                relation.junctionTableName + '.' +
                relatedModelRelation.junctionColumn,
                relatedModelName + '.' + relatedModelRelation.column
              )
              .where(
                relation.junctionTableName + '.' +
                relation.junctionColumn,
                entity[relation.column]
              );
          } else {
            const searchObject = {};

            searchObject[relatedModelRelation.column] =
              entity[relation.column];

            relatedEntities = await relatedModel
              .find(searchObject);
          }

          set(
            entity,
            ['_relations', relatedModelName],
            relatedEntities
          );

          if (next) {
            await Promise.all(
              relatedEntities.map(relatedEntity => this._findRelated(
                relatedEntity,
                next,
                relatedModel
              ))
            );
          }
        } else {
          const searchObject = {};

          searchObject[relatedModelRelation.column] =
            entity[relation.column];

          const relatedEntity = await relatedModel
            .findOne(searchObject);

          if (relatedEntity) {
            set(
              entity,
              ['_relations', relatedModelName],
              relatedEntity
            );

            if (next) {
              await this._findRelated(relatedEntity, next, relatedModel);
            }
          }
        }
      }
    }
  }

  async prepareArgs(args = {}) {
    const keys = Object.keys(args);

    for (const key of keys) {
      if (
        this.specs.columns.hasOwnProperty(key) &&
        this.specs.columns[key].type === 'password'
      ) {
        args[key] = Buffer.from(await bcrypt.hash(args[key], 10), 'binary');
      }
    }

    return args;
  }

  parseArgs(query, args = {}, property = 'where') {
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

    let result = query;
    const keys = Object.keys(args);

    for (let i = 0; i < keys.length; i += 1) {
      const key = keys[i];
      const method = i !== 0 ? property : 'where';

      if (key === '_or') {
        result = result[method](builder =>
          this.parseArgs(
            builder,
            args._or,
            'orWhere'
          )
        );
      } else if (key === '_and') {
        result = result[method](builder =>
          this.parseArgs(
            builder,
            args._and,
            'where'
          )
        );
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
          result = result[method](key, '=', args[key]);
        }
      }
    }

    if (orderColumn !== false) {
      result = result.orderBy(orderColumn, orderType);
    }

    result = result.limit(limit);
    result = result.offset(offset);

    return result;
  };

  _findBase(args = {}, related = []) {
    let query = this.parseArgs(
      this.knex
        .select('*')
        .from(this.specs.tableName),
      args
    );

    if (related.length > 0) {
      query = query.then(result => new Promise((resolve, reject) => {
        Promise.all(
          result.map(entity => Promise.all(
            related.map(
              relation => this._findRelated(entity, relation)
            )
          ))
        ).then(() => resolve(result));
      }));
    }

    return query;
  }

  find(args = {}, related = []) {
    return (
      this.prepareArgs(args)
        .then(args => this._findBase(args, related))
        .then(result => result)
    );
  }

  findOne(args = {}, related = []) {
    args._limit = 1;

    return (
      this.prepareArgs(args)
        .then(args => this._findBase(args, related))
        .then(result => result[0])
    );
  }

  delete(args = {}) {
    return (
      this.prepareArgs(args)
        .then(args => this.parseArgs(this.knex(this.specs.tableName), args))
        .del()
        .then(result => result)
    );
  }

  update(where = {}, set = {}) {
    if (this.specs.hasTimestamps) {
      const date = new Date();

      // date.setTime(date.getTime() + date.getTimezoneOffset() * 60000);

      set.updated_at = date;
    }

    return (
      this.prepareArgs(where)
        .then(args => this.parseArgs(
          this.knex(this.specs.tableName).update(set),
          args
        ))
        .then(result => result)
    );
  }

  create(value = {}) {
    if (this.specs.hasTimestamps) {
      const date = new Date();

      // date.setTime(date.getTime() + date.getTimezoneOffset() * 60000);

      set.created_at = date;
      set.updated_at = date;
    }

    return (
      this.prepareArgs(value)
        .then(args => this.knex(this.specs.tableName).insert(value))
    );
  }

  count(args = {}, column = '*') {
    return (
      this.prepareArgs(args)
        .then(args => this.parseArgs(
          this.knex.count(column).from(this.specs.tableName),
          args
        ))
        .then(a => a[0][`count(${column})`])
    );
  }

  sum(column, args = {}) {
    return (
      this.prepareArgs(args)
        .then(args => this.parseArgs(
          this.knex.sum(column).from(this.specs.tableName),
          args
        ))
        .then(a => a[0][`sum(\`${column}\`)`] || 0)
    );
  }
};

const initializeColumn = (table, name, column) => {
  switch (column.type) {
    case 'text':
      return table.text(name, column.textType);

    case 'password':
      return table.binary(name, 60);

    case 'binary':
      return table.binary(name, column.length);

    case 'string':
    case 'varchar':
      return table.string(name, column.length);

    case 'int':
    case 'integer':
      return table.integer(name);

    case 'float':
    case 'decimal':
      return table[column.type](name, column.precision, column.scale);

    case 'datetime':
    case 'timestamp':
      return table[column.type](name, column.options);

    case 'time':
      return table.time(name, column.precision);

    case 'enumerable':
    case 'enum':
    case 'enu':
      return table.enu(name, column.values, column.options);

    default:
      return table[column.type](name);
  }
};

const heavyInit = (table, name, column, uniques) => {
  let dbColumn = initializeColumn(table, name, column);

  if (column.unique) {
    uniques.push(name);
  }

  if (column.notNull) {
    dbColumn = dbColumn.notNullable();
  }

  if (column.default) {
    dbColumn = dbColumn.defaultTo(column.default);
  }

  return dbColumn;
};

export const addModel = (key, model) => _models[key] = model;

export const query = (name, origin = 'unspecified') => {
  return get(_models, [origin, name], null);
};

export const queryAll = origin => {
  if (origin) {
    return _models[origin];
  }

  return _models;
}

function __default(knex, models, printReady = true) {
  const relations = {};

  if (!hasInitialized) {
    hasInitialized = true;

    return __default(knex, [{
      tableName: UTILITY_DATABASE_NAME,
      origin: 'utility',
      columns: {
        table_name: {
          type: 'string'
        }
      }
    }], false).then(() => __default(knex, models));
  }

  return knex
    .transaction(trx => {
      const checkTable = (model) => trx.schema
        .hasTable(model.tableName)
        .then(exists => {
          if (exists) {
            return trx.raw(`describe ${model.tableName}`)
              .then(description => {
                description = description[0];

                return trx.schema.alterTable(model.tableName, table => {
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
                          if (!model.hasOwnProperty('relations')) {
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
                });
              });
          } else {
            return trx.schema.createTable(model.tableName, table => {
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
            });
          }
        });
      const relationColumn = (
        relation,
        tableFromName,
        tableToName
      ) => trx.schema.hasColumn(
        tableFromName,
        relation.from.column
      ).then(exists => (
        exists ?
          trx.schema.alterTable(tableFromName, table => {
            if (relation.from.notNull) {
              table
                .integer(relation.from.column)
                .unsigned()
                .notNull()
                .alter();
            } else {
              table
                .integer(relation.from.column)
                .unsigned()
                .alter();
            }
          }) :
          trx.schema.table(tableFromName, table => {
            if (relation.from.notNull) {
              table
                .integer(relation.from.column)
                .unsigned()
                .notNull();
            } else {
              table
                .integer(relation.from.column)
                .unsigned();
            }

            table.foreign(relation.from.column).references(
              `${tableToName}.${relation.to.column}`
            );
          })
      ))
        .catch(e => {
          console.log(e.sqlMessage || e);
        });

      return Promise.all(
        models.map(model => new Promise((resolve, reject) => {
          const res = () => {
            if (!_models.hasOwnProperty(model.origin)) {
              _models[model.origin] = {};
            }

            if (!_models[model.origin].hasOwnProperty(
              model.suffixTableName
            )) {
              _models[model.origin][model.suffixTableName] = {};
            }

            _models[model.origin][model.suffixTableName] =
              new KnexManageModel(knex, model);

            resolve();
          };

          if (!model.hasOwnProperty('origin')) {
            model.origin = 'unspecified';
          }

          if (model.hasOwnProperty('relations')) {
            const access = model.tableName + ',' + model.origin;
            const resultObject = {};

            if (!relations.hasOwnProperty(access)) {
              relations[access] = {};
            }

            for (const relation of model.relations) {
              if (typeof relation.with === 'string') {
                relation.with = [relation.with, 'unspecified'];
              }

              const withString = relation.with[0] + ',' + relation.with[1];
              resultObject[withString] = relation;

              if (relation.type === 'one:one') {
                if (relation.from) {
                  if (!relations[access].hasOwnProperty(withString)) {
                    relations[access][withString] = {};
                  }

                  relations[access][withString].from = relation;
                } else {
                  if (!relations.hasOwnProperty(withString)) {
                    relations[withString] = {};
                    relations[withString][access] = {};
                  } else if (
                    !relations[withString].hasOwnProperty(access)
                  ) {
                    relations[withString][access] = {};
                  }

                  relations[withString][access].to = relation;
                }
              } else {
                if (relations.hasOwnProperty(withString)) {
                  relations[withString][access] = {
                    to: relation
                  };
                } else {
                  relations[access][withString] = {
                    from: relation
                  };
                }
              }
            }

            model.relations = resultObject;
          }

          model.suffixTableName = model.tableName;

          if (
            model.origin &&
            model.origin !== 'unspecified'
          ) {
            model.tableName = `${model.origin}_${model.tableName}`;
          }

          const util = query(UTILITY_DATABASE_NAME, 'utility');

          if (util) {
            util.findOne({
              table_name: model.tableName
            }).then(result => {
              if (!result) {
                util.create({
                  table_name: model.tableName
                });
              }

              checkTable(model).then(res);
            });
          } else {
            checkTable(model).then(res);
          }
        }))
      )
        .then(() => Promise.all((() => {
          const resultArray = [];

          for (const from in relations) {
            const fromModelName = from.split(',');
            const tableFrom = query(...fromModelName).specs;

            resultArray.push(Promise.all((() => {
              const resultArray = [];

              for (const to in relations[from]) {
                const toModelName = to.split(',');
                const tableTo = query(...toModelName).specs;

                resultArray.push(new Promise((resolve, reject) => {
                  const relation = relations[from][to];
                  const [typeFrom, typeTo] = (
                    relation.from ?
                      relation.from.type.split(':') :
                      relation.to.type.split(':').reverse()
                  );

                  if (!relation.hasOwnProperty('from')) {
                    relation.from = {};
                  }

                  if (!relation.hasOwnProperty('to')) {
                    relation.to = {};
                  }

                  if (!relation.from.hasOwnProperty('type')) {
                    relation.from.type = `${typeFrom}:${typeTo}`;
                  }

                  if (!relation.to.hasOwnProperty('type')) {
                    relation.to.type = `${typeTo}:${typeFrom}`;
                  }

                  if (!relation.from.hasOwnProperty('with')) {
                    relation.from.with = toModelName;
                  }

                  if (!relation.to.hasOwnProperty('with')) {
                    relation.to.with = fromModelName;
                  }

                  if (typeFrom === typeTo) {
                    if (typeTo === 'many') {
                      relation.from.junctionTableName =
                        relation.to.junctionTableName = (
                          tableFrom
                            .tableName
                            .localeCompare(tableTo.tableName) < 0 ?
                            `${tableFrom.tableName}_${tableTo.tableName}` :
                            `${tableTo.tableName}_${tableFrom.tableName}`
                        );

                      if (!relation.from.hasOwnProperty('column')) {
                        relation.from.column = 'id';
                      }

                      if (!relation.from.hasOwnProperty('junctionColumn')) {
                        relation.from.junctionColumn =
                          `${tableFrom.tableName}_id`;
                      }

                      if (!relation.to.hasOwnProperty('column')) {
                        relation.to.column = 'id';
                      }

                      if (!relation.to.hasOwnProperty('junctionColumn')) {
                        relation.to.junctionColumn =
                          `${tableTo.tableName}_id`;
                      }

                      trx.schema.hasTable(relation.to.junctionTableName)
                        .then(exists => {
                          if (exists || relation.to.doesJunctionTableExist) {
                            relation.to.doesJunctionTableExist = true;

                            trx.schema.alterTable(
                              relation.to.junctionTableName,
                              table => {
                                table.integer(
                                  relation.from.junctionColumn
                                ).unsigned().notNull().alter();

                                table.integer(
                                  relation.to.junctionColumn
                                ).unsigned().notNull().alter();
                              }
                            ).then(() => {
                              resolve();
                            });
                          } else {
                            relation.to.doesJunctionTableExist = true;

                            trx.schema.createTable(
                              relation.to.junctionTableName,
                              table => {
                                table.integer(
                                  relation.from.junctionColumn
                                ).unsigned().notNull();
                                table.foreign(
                                  relation.from.junctionColumn
                                ).references(
                                  `${tableFrom.tableName}.${relation.from.column}`
                                );

                                table.integer(
                                  relation.to.junctionColumn
                                ).unsigned().notNull();
                                table.foreign(
                                  relation.to.junctionColumn
                                ).references(
                                  `${tableTo.tableName}.${relation.to.column}`
                                );

                                table.primary([
                                  relation.from.junctionColumn,
                                  relation.to.junctionColumn
                                ]);
                              }
                            )
                              .then(resolve)
                              .catch(e => {
                                if (e.errno === 1050) {
                                  trx.schema.alterTable(
                                    relation.to.junctionTableName,
                                    table => {
                                      table.integer(
                                        relation.from.junctionColumn
                                      ).unsigned().notNull().alter();

                                      table.integer(
                                        relation.to.junctionColumn
                                      ).unsigned().notNull().alter();
                                    }
                                  ).then(resolve);
                                } else {
                                  console.log(
                                    'Error occurred while loading database:'
                                  );
                                  console.log(e.sqlMessage);
                                }
                              });
                          }
                        });
                    } else {
                      if (!relation.from.hasOwnProperty('column')) {
                        relation.from.column = tableTo.tableName + '_id';
                      }

                      if (!relation.to.hasOwnProperty('column')) {
                        relation.to.column = 'id';
                      }

                      relationColumn(
                        relation,
                        tableFrom.tableName,
                        tableTo.tableName
                      ).then(() => {
                        resolve();
                      });
                    }
                  } else {
                    let tableFromName, tableToName;

                    if (typeTo === 'one') {
                      tableFromName = tableFrom.tableName;
                      tableToName = tableTo.tableName;
                    } else {
                      [
                        relation.from,
                        relation.to,
                        tableFromName,
                        tableToName
                      ] =
                        [
                          relation.to,
                          relation.from,
                          tableTo.tableName,
                          tableFrom.tableName
                        ];
                    }

                    if (!relation.from.hasOwnProperty('column')) {
                      relation.from.column = tableToName + '_id';
                    }

                    if (!relation.to.hasOwnProperty('column')) {
                      relation.to.column = 'id';
                    }

                    relationColumn(
                      relation,
                      tableFromName,
                      tableToName
                    ).then(() => {
                      resolve();
                    });
                  }

                  tableTo.relations[from] = relation.from;
                  tableFrom.relations[to] = relation.to;
                }));
              }

              return resultArray;
            })()));
          }

          return resultArray;
        })()))
        .then(trx.commit)
        .catch(e => {
          console.log(e);
          trx.rollback();
        })
    })
    .then(() => {
      if (printReady) {
        console.log('Database loaded!');
      }
    })
    .catch(e => console.log(e));
};

export default __default;
