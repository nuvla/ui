# Nuvla Web UI

[![Build Status](https://travis-ci.com/nuvla/ui.svg?branch=master)](https://travis-ci.com/nuvla/ui)

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
    the UI served by nginx.  Available from the [nuvla/ui
    repository](https://hub.docker.com/r/nuvla/ui) on Docker Hub.

## Installation

The ui can be installed on its own of as part of a full Nuvla stack. Stand alone installations
must be configured to point to an existing Nuvla installation (see 
[Development Environment](#development-environment) section for details).

For full stack installation, please check instructions in the main
[Nuvla repository](https://github.com/nuvla/nuvla).

## Contributing

### Source Code Changes

To contribute code to this repository, please follow these steps:

 1. Create a branch from master with a descriptive, kebab-cased name
    to hold all your changes.

 2. Follow the developer guidelines concerning formatting, etc. when
    modifying the code.
   
 3. Once the changes are ready to be reviewed, create a GitHub pull
    request.  With the pull request, provide a description of the
    changes and links to any relevant issues (in this repository or
    others). 
   
 4. Ensure that the triggered CI checks all pass.  These are triggered
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
entries should be aligned**.

Additional, formatting guidelines, not handled by the Cursive plugin:

 - Use a new line after the `:require` and `:import` keys in namespace
   declarations.

 - Alphabetize the required namespaces.  This can be automated with
   `lein nsorg --replace`.

 - Use 2 blank lines between top-level forms.

 - Use a single blank line between a block comment and the following
   code.

IntelliJ (with Cursive) can format easily whole directories of source
code.  Do not hesitate to use this feature to keep the source code
formatting standardized.

## Frameworks and Libraries

Nuvla UI dependency:

 * [Nuvla Clojure(Script)
   API](https://github.com/nuvla/clojure-api): Provides
   a ClojureScript API to interface with the Nuvla server.

Frameworks:

 * [Reagent](https://github.com/reagent-project/reagent): Provides a
   ClojureScript interface to the
   [React](https://facebook.github.io/react/) framework.
 * [re-frame](https://github.com/Day8/re-frame): "a pattern for writing
   SPAs in ClojureScript, using Reagent".

Widgets:

 * [Semantic UI React](https://react.semantic-ui.com/introduction):
   React integration for Semantic UI.


## Development Environment

The essentials for using the development environment are below.

### Nuvla local stack

TODO...

### Browser

To test the code on a Nuvla server (e.g. https://nuv.la/) running
on a different machine, you'll need to start a browser with the XSS
protections disabled.

For **Chrome** on MacOS, this can be done with:

```
$ open /Applications/Google\ Chrome.app \
       --args --disable-web-security --user-data-dir
```

For **Safari**, first enable the "Develop" menu:

 * Open the Safari preferences,
 * Click the "Advanced" tab, and
 * Then activate the "Show Develop menu in menu bar" option.

Once the "Develop" menu is visible, **choose the "Disable Cross-Origin
Restrictions" option**.

There may be **FireFox** plugins that will allow you to disable the
CORS protections.  The easier solution is to use Chrome or Safari.

### NPM

The build uses [shadow-cljs](http://shadow-cljs.org/) to facilitate
the use of Javascript modules packaged with NPM.  This requires that
you install the `npm` command line interface on your development
machine.

On Mac OS, the `npm` command comes with the Node.js distribution of
Homebrew.  Just run the command `brew install node`.

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

 1. Run `npm install` at the root of the cloned repository.  This only
    needs to be done once at the beginning and then whenever
    dependencies change.

 2. Start a development server for the build with `lein dev`.  When
    this completes ("build completed" message in the terminal), you
    can then connect to the process on http://localhost:8280.

 3. Changes you make to the code should automatically be recompiled
    and then pushed to your browser.

 4. If you need a REPL, you can run the command `lein cljs-repl` from
    a different terminal.

 5. You can terminate the process with Ctrl-C from the terminal window. 

## Integration with IntelliJ

You can import the repository through IntelliJ, using the "leiningen"
method in the dialog.

If you have the IntelliJ command line installed, the shadow-cljs
heads-up display, should open files in the IntelliJ editor.

The command for opening Chrome with the security disabled, can be
configured as an "external tool" in IntelliJ.  In "Preferences", go to
the "Tools" -> "External Tools" panel.

### Logging

You can reset the logging level for kvlt from the REPL when running
in development mode. From the REPL do:

```
=> (require '[taoensso.timbre :as timbre])
=> (timbre/set-level! :info)
```

The default value is `:debug` which will log all of the HTTP requests
and responses.  This is useful when debugging interactions with
Nuvla, but annoying otherwise.

## Release Process

Release process instructions are available [here](RELEASE.md).

## Copyright

Copyright &copy; 2019, SixSq Sàrl

## License

Licensed under the Apache License, Version 2.0 (the "License"); you
may not use this file except in compliance with the License.  You may
obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
implied.  See the License for the specific language governing
permissions and limitations under the License.
