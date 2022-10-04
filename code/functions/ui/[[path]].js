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

  let { path } = params;
  console.log(path);
  let [firstPathPart, file] = path;

  if (filesToFetchFromProd.includes(file)) {
    let productionEndpoint = 'https://nuvla.io';
    return fetch(`${productionEndpoint}${url.pathname.replace('/proxy/', '/')}`);
  }

  return fetch(url.toString().replace('/ui/', '/'), request);
}

const filesToFetchFromProd = [
  'semantic.min.css',
  'themes',
  'codemirror.css',
  'foldgutter.css',
  'react-datepicker.min.css',
  'version.css',
  'dialog.css',
  'matchesonscrollbar.css',
  'leaflet.css',
  'leaflet.draw.css',
  'images',
];
