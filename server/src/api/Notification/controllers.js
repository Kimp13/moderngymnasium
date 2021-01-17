export default async function (req, res) {
    if (req.body.endpoint) {
        mg.query('pushOptions').create({
            userId: req.jwtPayload.id,
            subscription: JSON.stringify(req.body)
        });

        res.send({});
        return;
    }

    res.throw(400);
};