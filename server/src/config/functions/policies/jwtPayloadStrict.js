module.exports = async (req, res) => {
  const sources = [
    (
      req.headers.authorization ?
        (
          req.headers.authorization.substring(0, 6) === 'Bearer' ?
            req.headers.authorization.substring(7) :
            req.headers.authorization
        ) :
        undefined
    ),
    (
      req.headers.authentication ?
        (
          req.headers.authentication.substring(0, 6) === 'Bearer' ?
            req.headers.authentication.substring(7) :
            req.headers.authentication
        ) :
        undefined
    ),
    req.cookies?.jwt,
    req.query?.jwt,
    req.body?.jwt
  ];

  for (const jwt of sources) {
    req.jwtPayload = await mg.services.jwt.verify(jwt);

    if (req.jwtPayload) return;
  }

  res.throw(401);
};