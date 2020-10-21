const { raw } = require("body-parser");
const { initial, hasIn } = require("lodash");

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

module.exports = knex => ({
  init: async models => {
    await Promise.all(
      models.map(model => new Promise((resolve, reject) => {
        const foreignKeys = [];

        knex.schema.hasTable(model.tableName).then(exists => {
          if (exists) {
            knex.raw('describe ' + model.tableName, description => {
              description = description[0];

              knex.schema.table(model.tableName, table => {
                for (const rawColumn of description) {
                  const initColumn = () => {
                    if (dbColumn === undefined) {
                      dbColumn = initializeColumn(table, rawColumn.Field, column);
                    }
                  };

                  const primaryKeys = [];
                  const uniques = [];
                  const column = model.columns[rawColumn.Field];
                  let dbColumn;
                  let type;

                  if (rawColumn.Type.substring(0, 7) === 'varchar') {
                    type = 'string';
                  } else if (rawColumn.Type.substring(0, 6) === 'binary') {
                    type = 'binary';
                  } else {
                    type = rawColumn.Type;
                  }

                  if (column.type !== type) {
                    initColumn();
                  }

                  if (rawColumn.Null === 'NO') {
                    if (column.notNull) {
                      initColumn();

                      dbColumn = dbColumn.notNullable();
                    }
                  } else {
                    if (!column.notNull) {
                      initColumn();

                      dbColumn = dbColumn.nullable();
                    }
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
                    initColumn();

                    dbColumn = dbColumn.increments();
                  } else if (column.unique) {
                    uniques.push(rawColumn.Field);
                  } else if (column.hasOwnProperty('foreign')) {
                    column.foreign.parentTable = model.tableName;
                    column.foreign.parentColumn = rawColumn.Field;

                    foreignKeys.push(column.foreign);
                  }

                  if (rawColumn.Default != column.default) {
                    initColumn();
                    
                    dbColumn = dbColumn.defaultTo(column.default);
                  }

                  if (dbColumn) {
                    dbColumn.alter();
                  }

                  if (uniques.length > 0) {
                    table.unique(uniques);
                  }
    
                  if (primaryKeys.length > 0) {
                    table.primary(primaryKeys);
                  }
                }
              })
            })
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
                const dbColumn = initializeColumn(table, key, column);

                if (column.increments) {
                  dbColumn = dbColumn.increments();
                  hasInlinePrimary = true;
                } else if (column.primary) {
                  primaryKeys.push(key);
                }

                if (column.unique) {
                  uniques.push(key);
                }

                if (column.hasOwnProperty('foreign')) {
                  column.foreign.parentTable = model.tableName;
                  column.foreign.parentColumn = key;
                  foreignKeys.push(column.foreign);
                }

                if (column.notNull) {
                  dbColumn = dbColumn.notNullable();
                }

                if (column.default) {
                  dbColumn = dbColumn.defaultTo(column.default);
                }
              }

              if (uniques.length > 0) {
                table.unique(uniques);
              }

              if (primaryKeys.length > 0) {
                table.primary(primaryKeys);
              }

              resolve();
            });
          }
        }).then(() => {
          return Promise.all(
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
          )
        });
      }))
    )
  }
});