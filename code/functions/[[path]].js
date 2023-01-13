export async function onRequest(context) {
  // Contents of context object
  let {
    request, // same as existing Worker API
    env, // same as existing Worker API
    params, // if filename includes [id] or [[path]]
  } = context;

  const url = new URL(request.url);
  if (!url.pathname.startsWith('/ui')) {
    url.pathname = '/ui' + url.pathname;
    return Response.redirect(url, 307);
  }

  return env.ASSETS.fetch(request);
}
