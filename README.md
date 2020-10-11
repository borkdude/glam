# Glam

A cross-platform package manager.

Work in progress, still in flux, not ready for production, breaking changes will happen.

## Goals

- Easy CI install of glam itself and the packages that it supports
- Cross platform: support for linux, macOS and Windows (without relying on PowerShell)
- Bring binaries into scope globally or just for one shell or directory
- Configuration using [EDN](https://github.com/edn-format/edn)

## Install

Currently, glam relies on `git` (for downloading package repos) and `tar` (optional, for
untarring `.tgz` archives) to be installed.

Navigate to the latest build on
[CircleCI](https://app.circleci.com/pipelines/github/borkdude/glam) and download
a binary for linux or macOS. Binaries for Windows are coming soon. Unzip the
binary, place it on your path and run:

``` clojure
$ glam-bin setup
```

This installs a shell helper script and pulls the latest glam [packages](https://github.com/glam-pm/packages).

To finish setup, add this to your `.bashrc` analog:

``` shell
source $HOME/.glam/scripts/glam.sh
```

To immediately start using glam, also execute the above in your shell.

After setting up, you will find a `glam.edn` in `$HOME/.config/glam` with the following contents:

``` clojure
{:glam/repos
 [{:repo/name glam/core
   :git/url "https://github.com/glam-pm/packages"}]
 :glam/deps {}}
```

Sample installation in a fresh Ubuntu docker image:

``` shell
$ docker run -it --rm ubuntu /bin/bash

# installing glam itself
$ apt-get update && apt-get install curl git unzip -y
$ curl -sLO https://30-298997735-gh.circle-artifacts.com/0/release/glam-0.0.1-SNAPSHOT-linux-amd64.zip
$ unzip glam-0.0.1-SNAPSHOT-linux-amd64.zip
$ mv glam-bin /usr/local/bin
$ source $HOME/.glam/scripts/glam.sh

# installing clj-kondo and babashka:
$ glam install clj-kondo/clj-kondo org.babashka/babashka
$ clj-kondo --version
clj-kondo v2020.09.09
$ bb --version
babashka v0.2.2
```

## Packages

Package files like `<package-org>/<package-name>.glam.edn` are listed in package
repos specified in the global `glam.edn` config file under
`:glam/repos`. Packages are cloned/pulled to `$HOME/.glam/packages`. The main
package repo lives in `$HOME/.glam/packages/glam/core`. You can add your own
repos in the config file and also change precedence by changing the order.

To update package repos, run:

``` clojure
$ glam pull
```

## Usage

## Current shell

To install packages for the current shell:

``` clojure
$ glam install clj-kondo/clj-kondo org.babashka/babashka
```

Now `clj-kondo` and `bb` are available:

``` clojure
$ which bb
/Users/borkdude/.glam/repository/org.babashka/babashka/0.2.2/bb
$ which clj-kondo
/Users/borkdude/.glam/repository/clj-kondo/clj-kondo/2020.09.09/clj-kondo
$ bb '(+ 1 2 3)'
6
```

Use `--verbose` for more output, `--force` for re-downloading packages.

To install a specific versions:

``` clojure
$ glam install clj-kondo/clj-kondo@2020.09.09 org.babashka/babashka@0.2.2
```

### Project

To save installation settings for a project directory, create a `glam.edn` with the following contents:

``` clojure
{:glam/deps {org.babashka/babashka "0.2.2"}}
```

To use the latest version, use `{org.babashka/babashka :latest}`.

Then run `glam install` and the deps should be added to the path.

### Global

To install packages globally, add to `:glam/deps` in `$HOME/.config/glam/glam.edn`:

``` clojure
{org.babashka/babashka "0.2.2}
```

Run `glam install` and the global package should be added to the path.

## Dev

To develop glam using Clojure, you can invoke it using `clojure -M:glam` when
adding this to `deps.edn`:

``` clojure
:glam {:extra-deps
       {borkdude/glam {:git/url "https://github.com/borkdude/glam"
                       :sha "<latest-sha>"}}
        :main-opts ["-m" "glam.main"]}
```

You can re-install shell scripts using `clojure -M:glam setup --force`.

You can override calling the binary in the shell script with `GLAM_CMD`, for example:

``` clojure
$ GLAM_CMD="clojure -M:glam" glam install
```

## License

Copyright © 2020 Michiel Borkent

Distributed under the EPL License. See LICENSE.
