# Analysis with SonarSource

## Dev environment

Prerequisites

```shell
brew install sonar-scanner
```

Make sure `project.clj` contains code analysis plugins and dependencies of your
choice and supported by `sonar-scanner`. E.g. (defaults):

```
  ;; the plugins can come from the parent
  :plugins [[lein-ancient "1.0.0-RC3"]
            [jonase/eastwood "1.2.2"]
            [lein-cloverage "1.2.2"]
            [lein-kibit "0.1.8"]
            [com.github.clj-kondo/lein-clj-kondo "0.1.3"]
            [lein-nvd "1.9.0"]]

:dependencies
  [
   ...
   [clj-kondo "RELEASE"]
   ]
```

To configure analysis, copy default `sonar-project.properties`
enable/disable/add parameters (see [sonarscanner](https://docs.sonarqube.org/latest/analysis/scan/sonarscanner/)).

Run analysis

```shell
sonar-scanner \
    -Dproject.settings=./my-sonar-project.properties \
    -Dsonar.host.url=https://sonarqube.cicd.nuv.la \
    -Dsonar.login=<token>
```
