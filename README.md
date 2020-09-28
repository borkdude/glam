# Glam

A cross-platform package manager for projects that rock.

Glam offers a flexible way to bring binaries into scope globally or for just one shell.

Work in progress, not ready for production, breaking changes will happen.

## Usage

Place `<package-org>/<package-name>.glam.edn` in your Clojure dependency.

Use this library using `clojure` with `deps.edn`:

``` clojure
{:aliases
 {:glam/packages {:extra-deps
                  {borkdude/glam {:git/url "https://github.com/borkdude/glam"
                                  :sha "4599fb019deae9418d76a9996ae19b4003f3cc96"}
                   ;; your-org/your-packages {,,,}
                   }
                  ;; :extra-paths ["your-packages"]
                  }
  :glam {:main-opts ["-m" "glam.main"]}}}
```

Use any later SHA at your convenience. Create an alias to reduce verbosity and
store it in your favorite `.bashrc` analog:

``` clojure
alias glam='clojure -M:glam/packages:glam'
```

E.g. in the glam repo's `packages` directory, there is `org.babashka/babashka.glam.edn`:

``` clojure
{:package/name org.babashka/babashka
 :package/description ""
 :package/version "0.2.1"
 :package/license ""
 :package/artifacts
 [{:os/name "Mac.*"
   :os/arch "x86_64"
   :artifact/url "https://github.com/borkdude/babashka/releases/download/v0.2.1/babashka-0.2.1-macos-amd64.zip"
   :artifact/executables ["bb"]}
  {:os/name "Linux.*"
   :os/arch "amd64"
   :artifact/url "https://github.com/borkdude/babashka/releases/download/v0.2.1/babashka-0.2.1-linux-amd64.zip"
   :artifact/executables ["bb"]}
  {:os/name "Windows.*"
   :os/arch "amd64"
   :artifact/url "https://github.com/borkdude/babashka/releases/download/v0.2.1/babashka-0.2.1-windows-amd64.zip"
   :artifact/executables ["bb.exe"]}]}
```

To install packages and get a path in return:

``` clojure
$ glam --install clj-kondo/clj-kondo org.babashka/babashka
/Users/borkdude/.glam/repository/clj-kondo/clj-kondo/2020.09.09:/Users/borkdude/.glam/repository/org.babashka/babashka/0.2.1
```

Use `--verbose` for more output, `--force` for re-downloading packages.

The resulting path can then be used to add programs on the path for the current shell:

``` clojure
$ export PATH=$(glam --install clj-kondo/clj-kondo org.babashka/babashka):$PATH
$ which bb
/Users/borkdude/.glam/repository/org.babashka/babashka/0.2.1/bb
$ which clj-kondo
/Users/borkdude/.glam/repository/clj-kondo/clj-kondo/2020.09.09/clj-kondo
$ bb '(+ 1 2 3)'
6
```

### Global

To install packages globally, use `--global`. This writes a path of globally installed packages to `$HOME/.glam/path`:

``` clojure
$ glam --install clj-kondo/clj-kondo --global --verbose
...
Wrote /Users/borkdude/.glam/path
/Users/borkdude/.glam/repository/clj-kondo/clj-kondo/2020.09.09
```

Add this path to `$PATH` in your favorite `.bashrc` analog:

``` clojure
# glam
alias glam='clojure -M:glam/packages:glam'
alias glam_path='export PATH="`cat $HOME/.glam/path 2>/dev/null`:$PATH"'
glam_path
```

Run `glam_path` again after installing to update the path for the current shell.

### Babashka

Glam can also run with [babashka](https://github.com/borkdude/babashka) for fast startup. Currently you will need the latest `0.2.2-SNAPSHOT` version. First install it using `clojure`:

``` clojure
$ glam --install org.babashka/babashka@0.2.2-SNAPSHOT --global --verbose
...
Wrote /Users/borkdude/.glam/path
/Users/borkdude/.glam/repository/org.babashka/babashka/SNAPSHOT:/Users/borkdude/.glam/repository/clj-kondo/clj-kondo/2020.09.09
```

Update the current shell's path:

``` clojure
$ glam_path
```

Now we can run glam using babashka:

``` clojure
$ bb -cp $(clojure -A:glam/packages -Spath) -m glam.main --install clj-kondo/clj-kondo --global --verbose

...
Wrote /Users/borkdude/.glam/path
/Users/borkdude/.glam/repository/org.babashka/babashka/SNAPSHOT:/Users/borkdude/.glam/repository/clj-kondo/clj-kondo/2020.09.09
```

To use `bb` instead of `clojure` to invoke `glam`:

``` clojure
# glam
alias glam_path='export PATH="`cat $HOME/.glam/path 2>/dev/null`:$PATH"'
glam_path
alias glam='bb -cp $(clojure -A:glam/packages -Spath) -m glam.main'
```

Or, to bundle everything into one asset, use babashka's `--uberjar` option:

``` clojure
$ bb -cp $(clojure -A:glam/packages -Spath) -m glam.main --uberjar glam.jar
```

This uberjar contains all packages from the classpath and the package manager
itself. You can then run it from anywhere on your system:

``` clojure
$ mv glam.jar /tmp
$ cd /tmp
$ bb -jar glam.jar --install clj-kondo/clj-kondo --global --verbose
...
Wrote /Users/borkdude/.glam/path
/Users/borkdude/.glam/repository/org.babashka/babashka/SNAPSHOT:/Users/borkdude/.glam/repository/clj-kondo/clj-kondo/2020.09.09
```

## License

Copyright © 2020 Michiel Borkent

Distributed under the EPL License. See LICENSE.
