import getPermission from 'getPermission';
import { sort } from 'timsort';
import search from 'binary-search';

module.exports = {
  find: async (req, res) => {
    if (req.query.id) {
      const id = parseInt(req.query.id, 10);

      if (id) {
        const announcement = mg.query('announcement').findOne({ id });

        if (announcement) {
          const relation = mg.query('announcementClassRole').findOne({
            announcementId: announcement.id,
            roleId: req.user.roleId,
            classId: req.user.classId
          });

          if (relation) {
            res.send(announcement);
            return;
          }

          res.throw(403);
          return;
        }
      }

      res.throw(400);
    } else {
      const { limit = 25, skip = 0 } = req.query;

      const roleIds = getPermission(
        req.user.permissions,
        ['announcement', 'read']
      );

      let announcements = mg.knex
        .select('announcement.*')
        .from('announcement')
        .innerJoin(
          'announcementClassRole',
          'announcementClassRole.announcementId',
          'announcement.id'
        )
        .where(
          'announcementClassRole.classId',
          req.user.classId
        );

      if (Array.isArray(roleIds)) {
        announcements = announcements.andWhereIn(
          'announcementClassRole.roleId',
          roleIds.map(id => parseInt(id, 10))
        );
      } else if (roleIds === false) {
        res.statusCode = 200;
        res.end('[]');
      }

      announcements = await announcements
        .orderBy('announcement.id', 'desc')
        .offset(skip)
        .limit(limit);

      res.send(announcements);
    }
  },

  count: async (req, res) => {
    const count = (await mg.knex('announcement')
      .count('*')
      .innerJoin(
        'announcementClassRole',
        'announcementClassRole.announcementId',
        'announcement.id'
      )
      .where(
        'announcementClassRole.classId',
        req.user.classId
      )
      .andWhere(
        'announcementClassRole.roleId',
        req.user.roleId
      ))[0]['count(*)'];

    res.send(String(count));
  },

  create: async (req, res) => {
    if (
      req.body.text &&
      req.body.recipients
    ) {
      const explicit = req.body.recipients.constructor === Object;
      const usersMap = await mg.services.role.getUsersCreateMap(req.user);

      const map = (
        req.body.recipients === true ?
          usersMap :
          explicit ?
            req.body.recipients :
            null
      );

      if (map !== null) {
        if (explicit) {
          const compFunction = (a, b) => a - b;

          for (const roleId in map) {
            if (roleId in usersMap) {
              sort(map[roleId], compFunction);

              for (const classId of map[roleId]) {
                if (search(
                  usersMap[roleId],
                  parseInt(classId, 10),
                  compFunction
                ) < 0) {
                  console.log('throwing 1');
                  res.throw(403);
                  return;
                }
              }
            } else {
              res.throw(403);
              return;
            }
          }
        }

        const {
          announcementId,
          recipients
        } = await mg.knex.transaction(t =>
          mg.query('announcement').create({
            text: req.body.text,
            authorId: req.user.id
          }).then(announcement => {
            const recipientsArray = new Array();

            for (const roleId in map) {
              const numberRoleId = parseInt(roleId, 10);

              for (const classId of map[roleId]) {
                recipientsArray.push({
                  announcementId: announcement[0],
                  roleId: numberRoleId,
                  classId
                });
              }
            }

            return t.insert(recipientsArray)
              .into('announcementClassRole')
              .then(() => ({
                announcementId: announcement[0],
                recipients: recipientsArray
              }));
          })
        );

        const userIds = new Set();

        for (const recipient of recipients) {
          for (const role of mg.cache.roles) {
            const permission = getPermission(
              role.permissions,
              [
                'announcement',
                'create'
              ]
            );

            if (
              permission === true ||
              Array.isArray(permission) &&
              search(permission, recipient.roleId, function compare(a, b) {
                return a - b;
              })
            ) {
              const users = await mg.query('user').find({
                classId: recipient.classId
              }, {}, ['id']);

              for (const user of users) {
                userIds.add(user.id);
              }
            }
          }
        }

        const tokens = await mg.query('pushOptions').find({
          userId_in: Array.from(userIds)
        }, {});

        const notificationTokens = [];

        for (let i = 0; i < tokens.length; i += 1) {
          if (tokens[i].gcm) {
            notificationTokens.push(
              tokens[i].subscription
            );
          }
        }

        await mg.fbAdmin.messaging()
          .sendMulticast({
            data: {
              text: req.body.text,
              authorId: String(req.user.id),
              id: String(announcementId),
              createdAt: new Date().toISOString()
            },
            tokens: notificationTokens
          })
          .catch(e => {
            console.log(e);
          });

        res.status(200).send({});
        return;
      }
    }

    res.throw(400);
  }
};