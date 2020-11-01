export async function post (req, res, next) {
  const model = mg.query(req.params.tableName);

  try {
    await model.create(req.body);
    res.statusCode = 200;
    res.end('{}');
  } catch (e) {
    console.log(e);
    res.statusCode = 400;
    res.end('{}');
  }
};