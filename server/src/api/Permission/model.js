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

    target: {
      type: 'string',
      length: 128
    }
  },

  relations: [
    {
      type: 'many:many',
      with: 'role'
    }
  ]
};