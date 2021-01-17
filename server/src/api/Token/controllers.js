export default async (req, res) => {
  if (req.body.token) {
    mg.query('pushOptions').create({
      userId: req.jwtPayload.id,
      gcm: true,
      subscription: req.body.token
    });

    res.send({});
    return;
  }

  res.throw(400);
  return;
};