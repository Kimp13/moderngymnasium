import getPermission from "getPermission";

export default {
    findUserClassesIds(user) {
        const classesPermission = getPermission(user.permissions, [
            "class",
            "multiple",
        ]);

        if (classesPermission === true) {
            const retarr = [];

            for (const classId in mg.cache.classes)
                retarr.push(parseInt(classId, 10));

            return retarr;
        } else {
            const classes = Array.isArray(classesPermission)
                ? classesPermission.split(",")
                : [user.classId];

            for (let i = 0; i < classes.length; i += 1) {
                classes[i] = parseInt(classes[i], 10);
            }

            return classes;
        }
    },

    async findRolesClasses(roleId, user) {
        if (!(roleId in mg.cache.roleClassMap))
            return [];

        const classesIds = this.findUserClassesIds(user);
        const retarr = [];

        for (let i = 0; i < classesIds.length; i += 1)
            if (mg.cache.roleClassMap[roleId].has(classesIds[i]))
                retarr.push(classesIds[i]);

        return retarr;
    },
};
