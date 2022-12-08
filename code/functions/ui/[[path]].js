export async function onRequest(context) {
  // Contents of context object
  let {
    request, // same as existing Worker API
    env, // same as existing Worker API
    params, // if filename includes [id] or [[path]]
  } = context;
  const url = new URL(request.url);

  let { path } = params;
  let [firstPathPart, file] = path || [];

  if (filesToFetchFromProd.includes(file)) {
    let productionEndpoint = 'https://nuvla.io';
    return fetch(`${productionEndpoint}${url.pathname.replace('/proxy/', '/')}`);
  }

  return env.ASSETS.fetch(url.toString().replace('/ui/', '/'), request);
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
