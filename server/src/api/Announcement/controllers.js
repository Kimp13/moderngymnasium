const jsonify = require("../../../utils/searchToJson");
const getUser = require("../../../utils/getUser");
const getPermission = require("../../../utils/getPermission");
const { mapKeys } = require("lodash");

module.exports = {
  find: async (req, res) => {
    let { limit, offset } = jsonify(req.search);
    let user = await getUser(req.headers.authentication);

    if (!limit) {
      limit = 10;
    }

    if (!offset) {
      offset = 0;
    }

    if (user) {
      user = (await mg.knex('user').where({
        id: user.id
      }))[0];

      if (user) {
        const announcements = await mg.knex('announcement')
          .innerJoin(
            'announcement_class_role',
            'announcement_class_role.announcement_id',
            'announcement.id'
          )
          .where(
            'announcement_class_role.class_id',
            user.class_id
          )
          .andWhere(
            'announcement_class_role.role_id',
            user.role_id
          )
          .orderBy('id', 'desc')
          .offset(offset)
          .limit(limit);

        res.statusCode = 200;
        res.end(JSON.stringify(announcements));
        return;
      }

      res.statusCode = 401;
      res.end('{}');
      return;
    }

    res.statusCode = 400;
    res.end('{}');
    return;
  },

  count: async (req, res) => {
    let user = await getUser(req.headers.authentication);

    if (user) {
      user = (await mg.knex('user').where({
        id: user.id
      }))[0];

      if (user) {
        const count = (await mg.knex('announcement')
          .count('*')
          .innerJoin(
            'announcement_class_role',
            'announcement_class_role.announcement_id',
            'announcement.id'
          )
          .where(
            'announcement_class_role.class_id',
            user.class_id
          )
          .andWhere(
            'announcement_class_role.role_id',
            user.role_id
          ))[0]['count(*)'];

        res.statusCode = 200;
        res.end(JSON.stringify({
          count
        }));
        return;
      }

      res.statusCode = 401;
      res.end('{}');
      return;
    }

    res.statusCode = 400;
    res.end('{}');
    return;
  },

  create: async (req, res) => {
    console.log(req.body);

    if (
      req.body.text &&
      req.body.recipients instanceof Array &&
      req.body.recipients.length > 0
    ) {
      let user = await getUser(req.headers.authentication);
      const permission = getPermission(user.permissions, [
        'announcement',
        'create'
      ]);

      user.class_id = Number(user.class_id);

      if (
        user &&
        permission
      ) {
        if (permission !== true) {
          for (let i = 0, j; i < req.body.recipients.length; i += 1) {
            for (j = 0; j < permission.length; j += 1) {
              if (req.body.recipients[i] == permission[j]) {
                break;
              }
            }

            if (j === permission.length) {
              res.statusCode = 401;
              res.end('{}');
              return;
            }
          }
        }

        const announcementId = await new Promise((resolve, reject) => {
          mg.knex.transaction(t => {
            const date = new Date();

            t('announcement')
              .insert({
                text: req.body.text,
                created_at: date,
                author_id: user.id
              }).then(announcement => {
                const resolveContext = () => {
                  t.insert(recipientsArray)
                    .into('announcement_class_role')
                    .then(result => {
                      t.commit().then(commited => {
                        resolve(announcement[0]);
                      });
                    });
                };

                const recipientsCriteria = new Array();
                const recipientsArray = new Array();

                if (permission instanceof Array) {
                  for (let role of req.body.recipients) {
                    recipientsArray.push({
                      announcement_id: announcement[0],
                      class_id: user.class_id,
                      role_id: role
                    });
                  }

                  resolveContext();
                } else {
                  t.select('id')
                    .from('role')
                    .then(roles => {
                      for (let role of roles) {
                        recipientsArray.push({
                          announcement_id: announcement[0],
                          class_id: user.class_id,
                          role_id: role.id
                        });
                      }

                      resolveContext();
                    });
                }
              }, e => {
                console.log(e);
                t.rollback();
              });
          });
        });

        const data =
          await mg.services.announcement.getAnnouncementAtId(announcementId);

        data.created_at = data.created_at.toISOString();
        data.id = String(data.id);
        data.author_id = String(data.author_id);

        const tokens = await mg
          .services
          .announcement
          .getUsersTokens(announcementId);

        const message = {
          data,
          tokens
        };

        const response = await mg.firebaseAdmin.messaging().sendMulticast(message);
        console.log(response.successCount + ' messages were sent successfully');

        res.statusCode = 200;

        res.end('{}');
        return;
      }

      res.statusCode = 401;
      res.end('{}');
      return;
    }

    res.statusCode = 400;
    res.end('{}');
    return;
  }
};