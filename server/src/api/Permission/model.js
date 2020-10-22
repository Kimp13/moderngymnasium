module.exports = {
  tableName: 'permission',
  columns: {
    type: {
      type: 'string',
      length: 48,
      notNull: true
    },

    operation: {
      type: 'string',
      length: 48
    },

    role_id: {
      type: 'integer',
      foreign: {
        table: 'role',
        column: 'id'
      }
    }
  }
};