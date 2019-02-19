# SlipStream Web UI

[![Build Status](https://travis-ci.com/nuvla/ui.svg?branch=master)](https://travis-ci.com/nuvla/ui)

This repository provides a Docker image containing the Nuvla
browser-based user interface. This consists of Javascript code (built
with ClojureScript) and other support files (HTML, CSS, etc.).

## Frameworks and Libraries

Nuvla dependency:

 * [Nuvla Clojure(Script)
   API](https://github.com/nuvla/clojure-api): Provides
   a ClojureScript API for the CIMI and CIMI-like resources within
   SlipStream.

Frameworks:

 * [Reagent](https://github.com/reagent-project/reagent): Provides a
   ClojureScript interface to the
   [React](https://facebook.github.io/react/) framework.
 * [re-frame](https://github.com/Day8/re-frame): A framework that
   relies on React for visualization and provides mechanisms for using
   "FRP" for data flow and control.

Widgets:

 * [Semantic UI React](https://react.semantic-ui.com/introduction):
   React integration for Semantic UI.


## Development Environment

The essentials for using the development environment are below.

### Browser

To test the code on a SlipStream server (e.g. https://nuv.la/) running
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
SlipStream, but annoying otherwise.

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
