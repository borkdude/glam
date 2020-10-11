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

## Usage

Package files like `<package-org>/<package-name>.glam.edn` are listed in package
repos downloaded to `$HOME/.glam/packages`. The main package repo is in
`$HOME/.glam/packages/glam/core`, but you can add your own repos in a config file (docs for this will come).

E.g. in`packages/glam/core` there is a `org.babashka/babashka.glam.edn`.

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

### Global

To install packages globally, use `--global` or `-g`.

``` clojure
$ glam install clj-kondo/clj-kondo -g --verbose
...
Wrote /Users/borkdude/.glam/path
```

<!-- ### Babashka -->

<!-- Glam can also run with [babashka](https://github.com/borkdude/babashka) for fast -->
<!-- startup. You'll need version `0.2.2` or higher. First install it using `glam`: -->

<!-- ``` clojure -->
<!-- $ glam org.babashka/babashka -g -->
<!-- ``` -->

<!-- Glam automatically detects if you have a compatible `bb` installed, so next -->
<!-- `glam` invocations are invoked using `bb`: -->

<!-- ``` clojure -->
<!-- $ time (glam) -->
<!-- ( glam; )   0.03s  user 0.03s system 93% cpu 0.065 total -->
<!-- ``` -->

<!-- #### Uberjar -->

<!-- To bundle the package manager and packages into one asset e.g. for moving to another machine, use -->
<!-- babashka's `--uberjar` option: -->

<!-- ``` clojure -->
<!-- $ bb -cp $(clojure -Spath -A:glam) -m glam.main --uberjar glam.jar -->
<!-- ``` -->

<!-- This uberjar contains all packages from the classpath and the package manager -->
<!-- itself. You can then run it from anywhere on your system: -->

<!-- ``` clojure -->
<!-- $ mv glam.jar /tmp -->
<!-- $ cd /tmp -->
<!-- $ bb -jar glam.jar install clj-kondo/clj-kondo -g --verbose -->
<!-- ... -->
<!-- Wrote /Users/borkdude/.glam/path -->
<!-- /Users/borkdude/.glam/repository/org.babashka/babashka/SNAPSHOT:/Users/borkdude/.glam/repository/clj-kondo/clj-kondo/2020.09.09 -->
<!-- ``` -->

## License

Copyright © 2020 Michiel Borkent

Distributed under the EPL License. See LICENSE.
