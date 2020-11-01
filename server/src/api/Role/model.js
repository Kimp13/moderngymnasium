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
      length: 128,
      notNull: true
    },

    name_ru: {
      type: 'string',
      length: 128,
      notNull: true
    }
  },

  relations: [
    {
      type: 'many:many',
      with: 'permission'
    },
    {
      type: 'one:many',
      with: 'user'
    }
  ]
};