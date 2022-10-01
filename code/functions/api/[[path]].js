export async function onRequest(context) {
  // Contents of context object
  let {
    request, // same as existing Worker API                           -> needed
    env, // same as existing Worker API                               -> needed for different api backends than nuvla.io
    params, // if filename includes [id] or [[path]]
    waitUntil, // same as ctx.waitUntil in existing Worker API
    next, // used for middleware or to fetch assets
    data, // arbitrary space for passing data between middlewares
  } = context;
  const url = new URL(request.url);

  const firstPartOfHost = url.host.split('.')[0];

  const procutionApiEndpoint = 'https://nuvla.io';

  // try get api endpoint from environment or staging for preview depoyments, if present
  let apiUrl =
    firstPartOfHost === 'nuvla-ui'
      ? procutionApiEndpoint
      : env[firstPartOfHost] || env['staging'] || 'https://nuvla.io';

  let response = await fetch(apiUrl + url.pathname, request);

  // override base-uri for /api/cloud-entry-point responses
  if (url.pathname.includes('cloud-entry-point')) {
    let body = await response.json();
    body = { ...body, 'base-uri': url.origin + '/api/' };
    return new Response(JSON.stringify(body));
  }

  return response;
}
