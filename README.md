# CPM

CPM offers a flexible way to bring binaries into scope globally or for just one shell.

Work in progress, not ready for production, breaking changes will happen.

## Usage

Place `<package-org>/<package-name>.cpm.edn` in your Clojure dependency.

E.g. in the CPM repo's `packages` directory, there is `org.babashka/babashka.cpm.edn`:

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

To create a path with packages:

``` clojure
$ clojure -M -m cpm.main --install clj-kondo/clj-kondo org.babashka/babashka
/Users/borkdude/.cpm/repository/clj-kondo/clj-kondo/2020.09.09:/Users/borkdude/.cpm/repository/org.babashka/babashka/0.2.1
```

Use `--verbose` for more output, `--force` for re-downloading packages.

The resulting path can then be used to add programs on the path for the current shell:

``` clojure
$ export PATH=$(clojure -M -m cpm.main --install clj-kondo/clj-kondo org.babashka/babashka):$PATH
$ which bb
/Users/borkdude/.cpm/repository/org.babashka/babashka/0.2.1/bb
$ which clj-kondo
/Users/borkdude/.cpm/repository/clj-kondo/clj-kondo/2020.09.09/clj-kondo
$ bb '(+ 1 2 3)'
6
```

### Global

To install packages globally, use `--global`. This writes a path of globally installed packages to `$HOME/.cpm/path`:

``` clojure
$ clojure -M -m cpm.main --install clj-kondo/clj-kondo --global --verbose
...
Wrote /Users/borkdude/.cpm/path
/Users/borkdude/.cpm/repository/clj-kondo/clj-kondo/2020.09.09
```

Add this path to `$PATH` in your favorite `.bashrc` analog:

``` clojure
# cpm
alias cpm_path='export PATH="`cat $HOME/.cpm/path 2>/dev/null`:$PATH"'
cpm_path
```

Run `cpm_path` again after installing to update the path for the current shell.

### Babashka

CPM can also run with [babashka](https://github.com/borkdude/babashka) for fast startup. Currently you will need the latest `0.2.2-SNAPSHOT` version. First install it using `clojure`:

``` clojure
$ clojure -M -m cpm.main --install org.babashka/babashka@0.2.2-SNAPSHOT --global --verbose
...
Wrote /Users/borkdude/.cpm/path
/Users/borkdude/.cpm/repository/org.babashka/babashka/SNAPSHOT:/Users/borkdude/.cpm/repository/clj-kondo/clj-kondo/2020.09.09
```

Update the current shell's path:

``` clojure
$ cpm_path
```

Now we can run CPM using babashka:

``` clojure
$ bb -cp src:packages -m cpm.main --install clj-kondo/clj-kondo --global --verbose
...
Wrote /Users/borkdude/.cpm/path
/Users/borkdude/.cpm/repository/org.babashka/babashka/SNAPSHOT:/Users/borkdude/.cpm/repository/clj-kondo/clj-kondo/2020.09.09
```

To package things up nicely, use babashka's `--uberjar` option:

``` clojure
$ bb -cp src:packages -m cpm.main --uberjar cpm.jar
```

This uberjar contains all packages from the classpath and the package manager
itself. You can then run it from anywhere on your system:

``` clojure
$ mv cpm.jar /tmp
$ cd /tmp
$ bb -jar cpm.jar --install clj-kondo/clj-kondo --global --verbose
...
Wrote /Users/borkdude/.cpm/path
/Users/borkdude/.cpm/repository/org.babashka/babashka/SNAPSHOT:/Users/borkdude/.cpm/repository/clj-kondo/clj-kondo/2020.09.09
```

## License

Copyright © 2020 Michiel Borkent

Distributed under the EPL License. See LICENSE.
