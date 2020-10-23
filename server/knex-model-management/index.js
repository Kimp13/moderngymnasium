const parseArgs = (query, args = {}, property = 'where') => {
  let result = query;
  const keys = Object.keys(args);

  for (let i = 0; i < keys.length; i += 1) {
    const key = keys[i];
    const method = i ? property : 'where';

    if (key === '_or') {
      result = result[method](builder => {parseArgs(
        builder,
        args._or,
        'orWhere'
      )});
    } else if (key === '_and') {
      result = result[method](builder => {parseArgs(
        builder,
        args._and,
        'where'
      )});
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

class KnexManageModel {
  constructor(knex, model) {
    this.model = model;
    this.knex = knex;
  }

  _findBase (args = {}) {
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

  find (args = {}) {
    return this._findBase(args);
  }

  findOne (args = {}) {
    args._limit = 1;

    return this._findBase(args).then(result => result[0]);
  }

  delete (args = {}) {
    return parseArgs(this.knex(model.tableName), args)
      .del();
  }

  update (where = {}, set = {}) {
    return parseArgs(this.knex(model.tableName), where)
      .update(set);
  }
};

const initializeColumn = (table, name, column) => {
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

const heavyInit = (table, name, column, uniques, foreigns) => {
  let dbColumn = initializeColumn(table, name, column);

  if (column.unique) {
    uniques.push(key);
  }

  if (column.hasOwnProperty('foreign')) {
    column.foreign.parentTable = model.tableName;
    column.foreign.parentColumn = key;
    foreigns.push(column.foreign);
  }

  if (column.notNull) {
    dbColumn = dbColumn.notNullable();
  }

  if (column.default) {
    dbColumn = dbColumn.defaultTo(column.default);
  }

  return dbColumn;
}

const _models = new Object();

module.exports = knex => ({
  init: models => {
    const foreignKeys = [];

    return new Promise((resolve, reject) => {
      Promise.all(
        Object.keys(models).map(key => new Promise((resolve, reject) => {
          const res = () => {
            _models[model.tableName] = new KnexManageModel(knex, model);
            resolve();
          };

          const model = models[key];

          knex.schema.hasTable(model.tableName).then(exists => {
            if (exists) {
              knex.raw(`describe ${model.tableName}`)
                .then(description => {
                  description = description[0];

                  knex.schema.alterTable(model.tableName, table => {

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
                          } else if (column.hasOwnProperty('foreign')) {
                            column.foreign.parentTable = model.tableName;
                            column.foreign.parentColumn = rawColumn.Field;

                            foreignKeys.push(column.foreign);
                          }

                          if (column.default) {
                            dbColumn = dbColumn.defaultTo(column.default);
                          }

                          dbColumn.alter();
                        } else {
                          table.dropColumn(rawColumn.Field);
                        }
                      }
                    }

                    for (const key of Object.keys(model.columns)) {
                      if (!hasRaw.hasOwnProperty(key)) {
                        const dbColumn = heavyInit(
                          table,
                          key,
                          model.columns[key],
                          uniques,
                          foreignKeys
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
              knex.schema.createTable(model.tableName, table => {
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
                    uniques,
                    foreignKeys
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
                    table.integer('id').increments();
                  }
                }

                res();
              });
            }
          })
        }))
      ).then(() => Promise.all(
        foreignKeys.map(key => new Promise((resolve, reject) => {
          knex.schema.table(key.parentTable, table => {
            table.foreign(key.parentColumn)
              .references(key.column)
              .inTable(key.table)
              .onUpdate(key.onUpdate || 'NO ACTION')
              .onDelete(key.onDelete || 'NO ACTION');

            resolve();
          });
        }))
      )).then(resolve);
    })
  },

  query: name => _models[name]
});