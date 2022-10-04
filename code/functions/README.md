# Cloudflare Preview Deployments

We use Cloudflare Pages for preview deployments of pushed branches and pull requests.

The files `api/[[path]].js` and `ui/[[path]].js` in the `functions` folder get picked up by Cloudflare and deployed as serverless function.
They are used as reverse proxies for api and static asset requests.

They are not used when developing locally or in production.

The same goes for the Github Actions workflow in `.github/workflows/prev_deploy.yml`: It is only used for preview deployments.