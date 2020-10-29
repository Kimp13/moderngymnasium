module.exports = {
  tableName: 'user',
  hasTimestamps: true,

  columns: {
    first_name: {
      type: 'string',
      length: 128
    },

    last_name: {
      type: 'string',
      length: 128
    },

    username: {
      type: 'string',
      length: 32,
      notNull: true,
      unique: true
    },

    password: {
      type: 'binary',
      length: 60,
      notNull: true
    }
  },

  relations: [
    {
      type: 'many:one',
      with: 'role'
    },

    {
      type: 'many:one',
      with: 'class'
    }
  ]
};