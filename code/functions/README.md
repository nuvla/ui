# Cloudflare Preview Deployments

We use Cloudflare Pages for preview deployments of pushed branches and pull requests.

## Functions

The files `api/[[path]].js` and `ui/[[path]].js` in the `functions` folder get picked up by Cloudflare and deployed as serverless function.
They are used as reverse proxies for api and static asset requests.

They are not used when developing locally or in production.

Current drawback: Traefik resets the `X-Forwarded-Host` header to the host of the backend server.
That's why we overwrite `location` and `base-uri` in response bodies.
It is working for the moment but could break in the future.

## GA workflow
The same goes for the Github Actions workflow in `.github/workflows/prev_deploy.yml`: It is only used for preview deployments.