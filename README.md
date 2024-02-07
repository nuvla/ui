# Nuvla Web UI

[![Build Status](https://github.com/nuvla/ui/actions/workflows/build.yml/badge.svg?branch=master)](https://github.com/nuvla/ui/actions/workflows/main.yml)

This repository contains the web user interface of the Nuvla solution. It is built as a modern
single page application.

The ui is built entirely in Clojurescript (that's cool), using [re-frame](https://github.com/Day8/re-frame)
and [reagent](https://github.com/reagent-project/reagent) as foundation, and
[Semantic UI](https://semantic-ui.com) for basic widgets and styling.

Our aim is to build a user experience such that users can start capturing, deploying and managing
containers on any virtualised environments (e.g. public cloud, private cloud and infrastructure,
as well as [NuvlaEdge](https://github.com/nuvlaedge) devices). And all this with no or minimum training.

More details on the overall Nuvla eco-system is available [here](https://github.com/nuvla/nuvla).

## Artifacts

- `nuvla/ui` - A Docker container containing the static content of
  the UI served by nginx. Available from the [nuvla/ui
  repository](https://hub.docker.com/r/nuvla/ui) on Docker Hub.

## Development Environment

### Installation

For development you need [nodejs](https://nodejs.org/), [leiningen](https://leiningen.org/)
and [caddy](https://caddyserver.com/)

On Mac OS, the `npm` command comes with the Node.js distribution of
Homebrew. Just run the command `brew install node`.

```bash
brew install node
brew install leiningen
brew install caddy
```

For other distributions or for direct installation on Mac OS, take a look at tools website for instructions.

The ui can be installed on its own or as part of a full Nuvla stack. Stand alone installations
must be configured to point to an existing Nuvla server.

You can also run your own Nuvla test server locally, which is great for testing. You'll find instructions
[here](https://github.com/nuvla/deployment/test).

### Choose or deploy a Nuvla server

You will need to point your development ui environment to a Nuvla server. For that
you have a few choices:

1. [Deploy a test server](https://github.com/nuvla/deployment/tree/master/test)
2. Point to an already deployed server (e.g. https://nuvla.io)

You then need to configure your local Nuvla ui to point to the Nuvla server (next section).

### Configure caddy for the right server

To run your UI dev server and your backend server from the same host address we use [Caddy](https://caddyserver.com/) as
a reverse-proxy during development. Caddy installs local certificates automatically, so we can run the development
environment on a "real" URL.

First, create a new file called `Caddyfile` with this content:

```
# this is a comment

{
    local_certs
}

nui.localhost {
    reverse_proxy localhost:8280
    reverse_proxy /api/* {
        to https://nuvla.io   # this points to a server of your chosing
        
        # uncomment following lines if your api server is using an untrusted certificate
        # transport http {
        #    tls_insecure_skip_verify 
        # }
    }
}
```

From the same directory where the `Caddyfile` is located, you run the command `caddy run` (or `caddy start` to run it in
the background).

If you want to point the API somewhere else, you can change your `Caddyfile`
from `reverse_proxy /api/* https://nuvla.io` to e.g. `reverse_proxy /api/* localhost:8200` and run the
command `caddy reload` (no need to restart anything else).


### Start development ui

Run `npm ci --legacy-peer-deps` inside `code` folder of the cloned repository. This only needs to be done once at the beginning and
then whenever dependencies change.
When you now run `lein dev` from the `code` folder, you can visit the Nuvla-Ui at https://nui.localhost.


## Testing

### End to end testing

We use [`playwright`](https://playwright.dev/) for e2e-testing.
To run playwright locally, inside the code directory, you `cp .env.e2e.TEMPLATE .env.e2e` and set the environment
variables specified in that file.
Tests are located in the `code/test/e2e/` directory.

#### Create tests

1. Run

```bash
npm run test:e2e:gen
```

you open the [playwright Test Generator](https://playwright.dev/docs/codegen).
It allows you to visit a website, record all interactions and let `playwright` generate all code necessary to replay
those interactions.

2. Copy the generated code into a new file.
   You can remove the code used to log in, as all tests run with a logged-in user (you can change the user in
   the `.env.e2e` file).
   Adjust the code to suit your needs, e.g., writing assertions using `expect`.

   As an alternative, you can save the output of the code generation directly to a file:
```bash
npm run test:e2e:gen -- -o test.spec.js
```

3. Put new tests inside the `code/test/e2e/loggedin` directory.
   The file has to end with `.spec.js` (`.ts` for typescript files is possible).

#### Run tests

Four additional commands are configured in `package.json` to run the tests (all starting with `test:e2e`).

Run all tests:

```bash
npm run test:e2e
```

Run a single file by specifying a path, treated as a regex:

```bash
npm run test:e2e logout
```

Watch the test folder and run a test if the test file changes:

```bash
npm run test:e2e:watch
```

Run tests in headed mode, i.e. a browser window opens and you can see the test run.

You have to specify a path; this example runs all tests:

```bash
npm run test:e2e:headed
```

The headed test runs are also available in watch mode.
You do not have to provide a path regex: the path is provided by the watching process.

```bash
npm run test:e2e:headed:watch
```

#### Run in parallel

It is possible to run the tests in parallel.
Because they share the same logged in session (through `global-setup.js`), it could be that tests fail if
the `logout.spec.js` tests runs first.
To prevent this, only run tests inside the loggedin folder.

```bash
npm run test:e2e loggedin -- --workers 5
```

#### Debug tests

By adding a `page.pause()` inside a test file and running in `headed` mode, you can stop and open an inspector view.

When a test fails, you find a `trace.zip` file inside the respective directory in the `code/test-report` folder. You can
open it by

```bash
npx playwright show-trace test-results/<path-to-test-file>-<test-name>-retry<retrynumber>/trace.zip
```

#### Find additional playwright settings

```bash
npx playwright --help
```

and

```bash
npx playwright test --help
```

### Unit tests

#### Run all unit tests

```bash
npm run test:unit
```

#### Run unit tests in cursive REPL

1. Shadow-clj build project:
    ```bash
    lein dev
    ```
2. Get nREPL server port from file or from console:
   - console: `shadow-cljs - nREPL server started on port 60325`
   - file: `code/.shadow-cljs/nrepl.port`
3. Wait until shadow-cljs finish compilation
4. Browse to UI page in web browser to load javascript runtime
5. Connect with nRepl client
6. Select project in nRepl, load test file and run-tests
   ```repl
   (shadow/repl :nuvla-ui)
   (load-file "test/cljs/sixsq/nuvla/ui/edges/utils_test.cljs")
   (cljs.test/run-tests 'sixsq.nuvla.ui.edges.utils-test)
   ```

## Bundle size analyze

shadow-cljs can create a bundle size report.

```bash
npx shadow-cljs run shadow.cljs.build-report nuvla-ui report.html
```

This saves the report as `report.html` in `resources/public/ui/js`.

## Contributing

### Source Code Changes

To contribute code to this repository, please follow these steps:

1. Create a branch from master with a descriptive, kebab-cased name
   to hold all your changes.

2. Follow the developer guidelines concerning formatting, etc. when
   modifying the code.

3. Once the changes are ready to be reviewed, create a GitHub pull
   request. With the pull request, provide a description of the
   changes and links to any relevant issues (in this repository or
   others).

4. Ensure that the triggered CI checks all pass. These are triggered
   automatically with the results shown directly in the pull request.

5. Once the checks pass, assign the pull request to the repository
   coordinator (who may then assign it to someone else).

6. Interact with the reviewer to address any comments.

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

The default value is `:debug` which will log all the HTTP requests
and responses. This is useful when debugging interactions with
Nuvla, but annoying otherwise.

## Release Process

Release process instructions are available [here](RELEASE.md).

## Copyright

Copyright &copy; 2019-2024, SixSq SA

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
