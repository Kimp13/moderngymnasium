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
    },

    name_ru: {
      type: 'string',
      length: 128
    }
  }
};