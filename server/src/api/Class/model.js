module.exports = {
  tableName: 'class',

  columns: {
    grade: {
      type: 'integer'
    },

    letter: {
      type: 'string',
      length: 1
    }
  },

  relations: [
    {
      type: 'one:many',
      with: 'user'
    }
  ]
};