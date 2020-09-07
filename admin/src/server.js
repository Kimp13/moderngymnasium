import dotenv from 'dotenv';
import sirv from 'sirv';
import polka from 'polka';
import compression from 'compression';
import * as sapper from '@sapper/server';

dotenv.config();

const { PORT, NODE_ENV } = process.env;
const dev = NODE_ENV === 'development';

polka() // You can also use Express
	.use(
		compression({ threshold: 0 }),
		sirv('static', { dev }),
		sapper.middleware({
      session: () => ({
        apiUrl: process.env.API_URL || 'http://localhost:3000'
      })
    })
	)
	.listen(PORT, err => {
		if (err) console.log('error', err);
	});
