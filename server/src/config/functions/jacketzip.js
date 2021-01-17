const webpush = require('web-push');
const tim = require('timsort');
const parsePermissions = require('../../../utils/permissionArrayToObject');

function parseAndSort(array) {
    for (let i = 0; i < array.length; i += 1) {
        array[i] = parseInt(array[i], 10);
    }

    tim.sort(array, function comparator(a, b) { return a - b; });
}

module.exports = function jacketzip() {
    async function updateCache() {
        mg.cache.classes = await mg.query('class').find();
        mg.cache.roles = await mg.query('role').find({
            _sort: 'id'
        }, ['permission']);

        for (const role of mg.cache.roles) {
            role.permissions = parsePermissions(role._relations.permission);

            if ('announcement' in role.permissions) {
                if ('create' in role.permissions.announcement) {
                    parseAndSort(role.permissions.announcement.create);
                }

                if ('read' in role.permissions.announcement) {
                    parseAndSort(role.permissions.announcement.read);
                }
            }

            delete role._relations;
        }
    }

    setInterval(updateCache, 300000);

    webpush.setVapidDetails(
        'mailto:akunec41@gmail.com',
        "BNYKuXiNeEasdlm9LJMUBl9ssPM9TkhsTRDXSnjyIxsq" +
        "YURjbT74PLW8BwN5henbxSponO2VP_NnqwZodKJAJHI",
        process.env.VAPID_PRIVATE_KEY
    );

    mg.push = webpush;

    return updateCache();
};