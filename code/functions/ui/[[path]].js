export async function onRequest(context) {
  // Contents of context object
  let {
    request, // same as existing Worker API
    env, // same as existing Worker API
    params, // if filename includes [id] or [[path]]
  } = context;
  const url = new URL(request.url);

  return env.ASSETS.fetch(url.toString().replace('/ui/', '/'), request);
}
