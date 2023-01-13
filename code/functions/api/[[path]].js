// hacky solution overwriting location and base-uri's in responses

export async function onRequest(context) {
  // Contents of context object
  let {
    request, // same as existing Worker API                           -> needed
    env, // same as existing Worker API                               -> needed for different api backends than nuvla.io
    params, // if filename includes [id] or [[path]]
  } = context;
  const url = new URL(request.url);

  const { path } = params;
  const [firstPathPart] = path || [];

  const apiEndpoint = env.API_ENDPOINT || 'https://nuvla.io';

  try {
    let response = await fetch(apiEndpoint + url.pathname, request);

    // override all location responses for /api/session
    let body = await response.json();
    // override base-uri for /api/cloud-entry-point responses
    if (firstPathPart === 'cloud-entry-point') {
      body = { ...body, 'base-uri': url.origin + '/api/' };
    }

    if (body.location && body.status != 202) {
      let locationUrl = new URL(body.location);
      locationUrl.host = url.host;
      locationUrl.protocol = url.protocol;
      body = { ...body, location: locationUrl };
    }
    let newResponse = new Response(JSON.stringify(body), { status: response.status });
    newResponse.headers.set('content-type', response.headers.get('content-type'));
    if (response.headers.has('set-cookie')) {
      newResponse.headers.set('set-cookie', response.headers.get('set-cookie'));
    }
    return newResponse;
  } catch (e) {
    console.error(e);
  }

  return env.ASSETS.fetch(request);
}
