export default async (req, res) => {
  if (req.body.token) {
    await mg.fbAdmin.messaging().send({
      token: req.body.token
    }, true)
      .then(good => {
        mg.query('pushOptions').create({
          userId: req.jwtPayload.id,
          gcm: true,
          subscription: req.body.token
        });

        res.send({});
      }, bad => {
        console.log(bad);
        res.throw(400);
      });

    return;
  }

  res.throw(400);
  return;
};