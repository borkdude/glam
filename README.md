# Glam

A cross-platform package manager for projects that rock.

Work in progress, still in flux, not ready for production, breaking changes will happen.

## Goals

- Easy CI install of glam itself and the packages that it supports
- Cross platform: support for linux, macOS and Windows (without relying on PowerShell)
- Bring binaries into scope globally or just for one shell or directory
- Configuration using [EDN](https://github.com/edn-format/edn)

## Install

Glam can currently be used as a Clojure library. There will probably be a
GraalVM compiled binary in the future.

Install glam by using this alias in `deps.edn`:

``` clojure
:glam {:extra-deps
       {borkdude/glam {:git/url "https://github.com/borkdude/glam"
                       :sha "92e1a8ec285bb983ad1c2fba28837606c5c99ab6"}}
        :main-opts ["-m" "glam.main"]}
```

Use any later SHA at your convenience or simply clone this project and use
`:local/root`.

Additionally, install a shell helper script and pull package repos by running
this and following the instructions:

``` shell
$ clojure -M:glam setup
Include this in your .bashrc analog to finish setup:

source $HOME/.glam/scripts/glam.sh
```

Scripts for Windows will follow. Meanwhile you can replace `glam` invocations by
`clojure -M:glam` and append the printed path to `%PATH%` yourself.

After setting up, you will find a `glam.edn` in `$HOME/.config/glam` with the following contents:

``` clojure
{:glam/repos
 [{:repo/name glam/core
   :git/url "https://github.com/glam-pm/packages"}]
 :glam/deps {}}
```

## Usage

Package files like `<package-org>/<package-name>.glam.edn` are listed in package
repos downloaded to `$HOME/.glam/packages`. The main package repo is in
`$HOME/.glam/packages/glam/core`, but you can add your own repos in the config file.

E.g. in`packages/glam/core` there is a `org.babashka/babashka.glam.edn`.

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

### Project

To save installation settings for a project directory, create a `glam.edn` with the following contents:

``` clojure
{:glam/deps {org.babashka/babashka "0.2.2"}}
```

To use the latest version, use `org.babashka/babashka :latest`.

Then run `glam install` and the deps should be added to the path.

### Global

To install packages globally, add to `:glam/deps` in `$HOME/.config/glam/glam.edn`:

``` clojure
{org.babashka/babashka "0.2.2}
```

To use the latest version, use `org.babashka/babashka :latest`.

Run `glam install` and the global package should be added to the path.

## License

Copyright © 2020 Michiel Borkent

Distributed under the EPL License. See LICENSE.
