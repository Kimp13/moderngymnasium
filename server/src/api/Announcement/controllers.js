const jsonify = require("../../../utils/searchToJson");
const getUser = require("../../../utils/getUser");
const getPermission = require("../../../utils/getPermission");
const permissionClasses = require('../../../utils/parseClasses');
const mapKeys = require('lodash/mapKeys');
const tim = require('timsort');
const binarySearch = require('binary-search');
const { toNumber } = require("lodash");

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
      user = await mg.query('user').findOne({
        id: user.id
      }, ['role.permission']);

      if (user) {
        const roleIds = getPermission(
          user._relations.role._relations.permission,
          ['announcement', 'read']
        );

        let announcements = mg.knex('announcement')
          .innerJoin(
            'announcement_class_role',
            'announcement_class_role.announcement_id',
            'announcement.id'
          )
          .where(
            'announcement_class_role.class_id',
            user.class_id
          );

        if (Array.isArray(roleIds)) {
          announcements = announcements.andWhereIn(
            'announcement_class_role.role_id',
            roleIds.map(id => parseInt(id, 10))
          );
        } else if (roleIds === false) {
          res.statusCode = 200;
          res.end('[]');
        }

        announcements = await announcements
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
      user = await mg.query('user').findOne({
        id: user.id
      });

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
    }

    res.statusCode = 401;
    res.end('{}');
    return;
  },

  create: async (req, res) => {
    if (
      req.body.text &&
      req.body.recipients &&
      typeof req.body.recipients === 'object'
    ) {
      const roleIds = Object.keys(req.body.recipients);

      if (roleIds.length > 0) {
        const user = await getUser(req.headers.authentication);
        const multipleClassPermission = getPermission(user.permissions, [
          'multiple_classes'
        ]);
        const createPermission = getPermission(user.permissions, [
          'announcement',
          'create'
        ]);

        if (
          user &&
          createPermission
        ) {
          const compFunction = (a, b) => a - b;
          user.class_id = Number(user.class_id);

          if (Array.isArray(createPermission)) {
            const throwErr = () => {
              res.statusCode = 401;
              res.end('{}');
            };

            const permittedClasses = (
              multipleClassPermission ?
                permissionClasses(multipleClassPermission) :
                [user.class_id]
            );

            tim.sort(createPermission, compFunction);
            tim.sort(permittedClasses, compFunction);

            for (let i = 0; i < roleIds.length; i += 1) {
              if (binarySearch(createPermission, roleIds[i], compFunction) >= 0) {
                for (const classId of req.body.recipients[roleIds[i]]) {
                  if (binarySearch(permittedClasses, classId, compFunction) < 0) {
                    throwErr();
                    return;
                  }
                }
              } else {
                throwErr();
                return;
              }
            }
          }

          const announcementId = await mg.knex.transaction(t => {
              const date = new Date();

              return t('announcement')
                .insert({
                  text: req.body.text,
                  created_at: date,
                  author_id: user.id
                }).then(announcement => {
                  const recipientsArray = new Array();

                  for (let i = 0; i < roleIds.length; i += 1) {
                    const numberRoleId = Number(roleIds[i]);

                    for (const class_id of req.body.recipients[roleIds[i]]) {
                      recipientsArray.push({
                        announcement_id: announcement[0],
                        role_id: numberRoleId,
                        class_id
                      });
                    }
                  }

                  return t.insert(recipientsArray)
                    .into('announcement_class_role')
                    .then(() => announcement[0]);
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
      }
    }

    res.statusCode = 400;
    res.end('{}');
    return;
  }
};