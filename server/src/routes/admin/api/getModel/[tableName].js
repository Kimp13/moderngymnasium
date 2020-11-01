import getUser from "getUser";
import pick from 'lodash/pick';
import omit from 'lodash/omit';
import defaults from 'lodash/defaults';

export async function get (req, res, next) {
  const user = await getUser(req.cookies.jwt);
  
  if (user) {
    const model = mg.query(req.params.tableName);

    if (model) {
      res.statusCode = 200;

      res.end(JSON.stringify(defaults(
        {
          relations: model.specs.relations.map(relation => relation.with)
        },
        model.specs
      )));

      return;
    }
    
    res.statusCode = 400;
    res.end('{}');
    return;
  }

  res.statusCode = 401;
  res.end('{}');
};