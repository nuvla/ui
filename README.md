# Nuvla Web UI

[![Build Status](https://github.com/nuvla/ui/actions/workflows/main.yml/badge.svg?branch=master)](https://github.com/nuvla/ui/actions/workflows/main.yml)

This repository contains the web user interface of the Nuvla solution. It is built as a modern
single page application.

The ui is built entirely in Clojurescript (that's cool), using [re-frame](https://github.com/Day8/re-frame)
and [reagent](https://github.com/reagent-project/reagent) as foundation, and
[Semantic UI](https://semantic-ui.com) for basic widgets and styling.

Our aim is to build a user experience such that users can start capturing, deploying and managing
containers on any virtualised environments (e.g. public cloud, private cloud and infrastructue,
as well as [NuvlaBox](https://github.com/nuvlabox) edge devices). And all this with no or minimum training.

More details on the overall Nuvla eco-system is available [here](https://github.com/nuvla/nuvla).

## Artifacts

- `nuvla/ui` - A Docker container containing the static content of
  the UI served by nginx. Available from the [nuvla/ui
  repository](https://hub.docker.com/r/nuvla/ui) on Docker Hub.

## Installation

For development you need [nodejs](https://nodejs.org/), [leiningen](https://leiningen.org/) and [caddy](https://caddyserver.com/)

The ui can be installed on its own or as part of a full Nuvla stack. Stand alone installations
must be configured to point to an existing Nuvla server (see
[Development Environment](#development-environment) section for details).

You can also run your own Nuvla test server locally, which is great for testing. You'll find instructions
[here](https://github.com/nuvla/deployment/test).

## Development Environment

The essentials for using the development environment are below.

### Choose or deploy a Nuvla server

You will need to point your development ui environment to a Nuvla server. For that
you have a few choices:

1.  [Deploy a test server](https://github.com/nuvla/deployment/tree/master/test)
2.  Point to an already deployed server (e.g. https://nuvla.io)

You then need to configure your local Nuvla ui to point to the Nuvla server (next section).

### Configure your ui for the right server

To run your UI dev server and your backend server from the same host address we use [Caddy](https://caddyserver.com/) as a reverse-proxy during development. Caddy installs local certificates automatically, so we can run the development environment on a "real" URL.

First, in your host file, after the `localhost entry`, add a new entry `local-nuvla.dev` (you can choose something else) pointing `127.0.0.1` or `localhost`, for example `local-nuvla.dev`:

```
127.0.0.1 local-nuvla.dev
```

Second, you create a new file called `Caddyfile` with this content:

```sh
{
	local_certs
}

local-nuvla.dev {
	reverse_proxy localhost:8280
	reverse_proxy /api/* https://nuvla.io   ## this points to a server of your chosing
}
```

From the same directory where the `Caddyfile` is located, you run the command `caddy run` (or `caddy start` to run it in the background).

Lastly, edit the file `shadow-cljs.edn` (we use shadow-cljs, which is installed when you `npm install` inside the `code` directory).
and modify the dev environment configuration to point to your host file entry, `local-nuvla.dev` in our case:

```
:dev        {:closure-defines  {sixsq.nuvla.ui.utils.defines/HOST_URL "https://local-nuvla.dev"}`
```

When you now run `lein dev` from the `code` folder, you can visit the Nuvla-Ui at https://local-nuvla.dev.

If you want to point the API somewhere else, you can change your `Caddyfile` from `reverse_proxy /api/* https://nuvla.io` to e.g. `reverse_proxy /api/* localhost:8200` and run the command `caddy reload` (no need to restart anything else).

## Contributing

### Source Code Changes

To contribute code to this repository, please follow these steps:

1.  Create a branch from master with a descriptive, kebab-cased name
    to hold all your changes.

2.  Follow the developer guidelines concerning formatting, etc. when
    modifying the code.

3.  Once the changes are ready to be reviewed, create a GitHub pull
    request. With the pull request, provide a description of the
    changes and links to any relevant issues (in this repository or
    others).

4.  Ensure that the triggered CI checks all pass. These are triggered
    automatically with the results shown directly in the pull request.

5.  Once the checks pass, assign the pull request to the repository
    coordinator (who may then assign it to someone else).

6.  Interact with the reviewer to address any comments.

When the reviewer is happy with the pull request, he/she will "squash
& merge" the pull request and delete the corresponding branch.

### Code Formatting

The bulk of the code in this repository is written in Clojurescript.

The formatting follows the standard formatting provided by the Cursive
IntelliJ plugin with all the default settings **except that map
and let entries should be aligned**.

Additional, formatting guidelines, not handled by the Cursive plugin:

- Use a new line after the `:require` and `:import` keys in namespace
  declarations.

- Alphabetize the required namespaces. This can be automated with
  `lein nsorg --replace`.

- Use 2 blank lines between top-level forms.

- Use a single blank line between a block comment and the following
  code.

IntelliJ (with Cursive) can format easily whole directories of source
code. Do not hesitate to use this feature to keep the source code
formatting standardized.

### NPM

The build uses [shadow-cljs](http://shadow-cljs.org/) to facilitate
the use of Javascript modules packaged with NPM. This requires that
you install the `npm` command line interface on your development
machine.

On Mac OS, the `npm` command comes with the Node.js distribution of
Homebrew. Just run the command `brew install node`.

For other distributions or for direct installation on Mac OS, take a
look at the Node.js [downloads](https://nodejs.org/en/download/)
page.

### Leiningen

The development environment requires
[`lein`](https://leiningen.org). Follow the instructions on the
Leiningen website to install the tool.

### Workflow

Once all of the development tools have been installed, the workflow is
as follows:

1.  Run `npm install` at the `code` folder of the cloned repository. This only
    needs to be done once at the beginning and then whenever
    dependencies change.

2.  Start a development server for the build with `lein dev` from within the `code` folder. When
    this completes ("build completed" message in the terminal), you
    can then connect to the process on https://local-nuvla.dev. If you haven't followed the
    [reverse proxy setup here](#configure-your-ui-for-the-right-server) then the UI is available at http://localhost:8280 (but without a working backend).

3.  Changes you make to the code should automatically be recompiled
    and then pushed to your browser.

4.  If you need a REPL, you can run the command `lein cljs-repl` from
    a different terminal.

5.  You can terminate the process with Ctrl-C from the terminal window.

## Integration with IntelliJ

You can import the repository through IntelliJ, using the "leiningen"
method in the dialog.

If you have the IntelliJ command line installed, the shadow-cljs
heads-up display, should open files in the IntelliJ editor.

The command for opening Chrome with the security disabled, can be
configured as an "external tool" in IntelliJ. In "Preferences", go to
the "Tools" -> "External Tools" panel.

### Logging

You can reset the logging level for kvlt from the REPL when running
in development mode. From the REPL do:

```
=> (require '[taoensso.timbre :as timbre])
=> (timbre/set-level! :info)
```

The default value is `:debug` which will log all of the HTTP requests
and responses. This is useful when debugging interactions with
Nuvla, but annoying otherwise.

## Release Process

Release process instructions are available [here](RELEASE.md).

## Copyright

Copyright &copy; 2019-2022, SixSq SA

## License

Licensed under the Apache License, Version 2.0 (the "License"); you
may not use this file except in compliance with the License. You may
obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
implied. See the License for the specific language governing
permissions and limitations under the License.
