const mg = global.mg;

module.exports = {
  find: async ctx => {
    console.log(ctx.url);

    return;
  },
  count: async ctx => {
    ctx.status = 200;
    ctx.body = {
      count: await mg.models.user.count()
    };

    return;
  }
};