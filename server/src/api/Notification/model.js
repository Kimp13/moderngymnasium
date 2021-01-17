export default {
  tableName: 'pushOptions',

  columns: {
    subscription: {
      type: 'text',
      notNull: true
    },

    gcm: {
      type: 'boolean'
    },

    expiresAt: {
      type: 'datetime'
    }
  },

  relations: [
    {
      with: 'user',
      type: 'many:one'
    }
  ]
};