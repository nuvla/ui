export async function onRequest(context) {
  // Contents of context object
  let {
    request, // same as existing Worker API
    env, // same as existing Worker API
    params, // if filename includes [id] or [[path]]
    waitUntil, // same as ctx.waitUntil in existing Worker API
    next, // used for middleware or to fetch assets
    data, // arbitrary space for passing data between middlewares
  } = context;
  const url = new URL(request.url);
  let productionEndpoint = 'https://nuvla.io/';

  if (GITIGNORED_ASSETS.some((path) => params.path.includes(path))) {
    // Go to nuvla.io because those assets are not part of repository
    // Do we want to change that?
    return fetch(`${productionEndpoint}${params.path}`);
  }
  url.pathname = url.pathname.replace('/ui/', '/');
  return fetch(url.toString(), request);

  // // Rewrite request to point to API URL. This also makes the request mutable
  // // so you can add the correct Origin header to make the API server think
  // // that this request is not cross-site.
  // request = new Request(apiUrl, request);
  // request.headers.set('Origin', new URL(apiUrl).origin);
  // let response = await fetch(request);

  // // Recreate the response so you can modify the headers
  // response = new Response(response.body, response);

  // // Set CORS headers
  // response.headers.set('Access-Control-Allow-Origin', url.origin);

  // // Append to/Add Vary header so browser will cache response correctly
  // response.headers.append('Vary', 'Origin');

  // return response;
}

const GITIGNORED_ASSETS = [
  '/ui/css/semantic.min.css',
  '/ui/css/themes',
  '/ui/css/codemirror.css',
  '/ui/css/foldgutter.css',
  '/ui/css/react-datepicker.min.css',
  '/ui/css/version.css',
  '/ui/css/dialog.css',
  '/ui/css/matchesonscrollbar.css',
  '/ui/css/leaflet.css',
  '/ui/css/leaflet.draw.css',
  '/ui/css/images',
];
