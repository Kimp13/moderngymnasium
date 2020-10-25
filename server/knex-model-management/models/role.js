module.exports = {
  tableName: 'role',

  columns: {
    type: {
      type: 'string',
      length: 128,
      notNull: true
    },

    name: {
      type: 'string',
      length: 128
    }
  },

  relations: [
    {
      type: 'one:many',
      with: ['user', 'kmm_admin']
    }
  ]
};