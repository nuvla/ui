async function onRequest(context) {
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
  let apiUrl = 'https://nuvla.io' + url.pathname;

  // Rewrite request to point to API URL. This also makes the request mutable
  // so you can add the correct Origin header to make the API server think
  // that this request is not cross-site.
  request = new Request(apiUrl, request);
  request.headers.set('Origin', new URL(apiUrl).origin);
  let response = await fetch(request);

  if (url.pathname.includes('cloud-entry-point')) {
    let body = await response.json();
    body = { ...body, 'base-uri': url.origin + '/api/' };
    return new Response(JSON.stringify(body));
  }

  // Recreate the response so you can modify the headers
  response = new Response(response.body, response);

  // Set CORS headers
  response.headers.set('Access-Control-Allow-Origin', url.origin);

  // Append to/Add Vary header so browser will cache response correctly
  response.headers.append('Vary', 'Origin');

  return response;
}

// Cloudflare supports the GET, POST, HEAD, and OPTIONS methods from any origin,
// and allow any header on requests. These headers must be present
// on all responses to all CORS preflight requests. In practice, this means
// all responses to OPTIONS requests.
const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Methods': '*',
  'Access-Control-Max-Age': '86400',
};

export let onRequestGet = onRequest;
export let onRequestPost = onRequest;
export let onRequestPut = onRequest;
export let onRequestPatch = onRequest;
export let onRequestDelete = onRequest;
export let onRequestHead = onRequest;

export async function onRequestOptions(request) {
  // Make sure the necessary headers are present
  // for this to be a valid pre-flight request
  let headers = request.headers;
  if (
    headers.get('Origin') !== null &&
    headers.get('Access-Control-Request-Method') !== null &&
    headers.get('Access-Control-Request-Headers') !== null
  ) {
    // Handle CORS pre-flight request.
    // If you want to check or reject the requested method + headers
    // you can do that here.
    let respHeaders = {
      ...corsHeaders,
      // Allow all future content Request headers to go back to browser
      // such as Authorization (Bearer) or X-Client-Name-Version
      'Access-Control-Allow-Headers': request.headers.get('Access-Control-Request-Headers'),
    };

    return new Response(null, {
      headers: respHeaders,
    });
  } else {
    // Handle standard OPTIONS request.
    // If you want to allow other HTTP Methods, you can do that here.
    return new Response(null, {
      headers: {
        Allow: 'GET, HEAD, POST, OPTIONS, PUT, DELETE, PATCH',
      },
    });
  }
}
