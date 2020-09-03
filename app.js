const Koa = require('koa'),
      app = new Koa();

app.use((ctx, next) => {
  const start = new Date();
  next().then(() => {
    const ms = Date.now() - start.getTime();
    console.log(
      start.toISOString(),
      '|',
      ctx.method,
      'on',
      ctx.url,
      'took',
      ms,
      'milliseconds'
    );
  });
});

app.use(ctx => {
  ctx.body = {
    message: 'Hello, World!'
  };
});

app.listen(1856);