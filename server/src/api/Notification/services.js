export default {
  push(token, announcement) {
    if (token.gcm) {
      return mg.fbAdmin.messaging()
        .send({
          data: announcement,
          token: token.subscription
        })
        .catch(e => {
          console.log(e);

          if (
            e.errorInfo.code ===
            'messaging/registration-token-not-registered'
          ) {
            return mg.knex
              .del()
              .from('pushOptions')
              .where('id', token.id);
          }
        });
    } else {
      console.log('No way I handle this!');
    }
  }
};