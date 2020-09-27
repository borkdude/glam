# CPM

CPM offers a flexible way to bring binaries into scope for a shell,
piggybacking on [tools deps](https://clojure.org/guides/deps_and_cli).

Work in progress, not ready for production, breaking changes will happen.

## Usage

Place `<package-name>.cpm.edn` in your Clojure dependency.

E.g. in the CPM repo's `test-resources` directory, there is `babashka.cpm.edn`:

``` clojure
{:package/name org.babashka/babashka
 :package/description ""
 :package/version "0.2.1"
 :package/license ""
 :package/artifacts
 [{:os/name "Mac.*"
   :os/arch "x86_64"
   :artifact/url "https://github.com/borkdude/babashka/releases/download/v0.2.1/babashka-0.2.1-macos-amd64.zip"
   :artifact/executables ["bb"]}]}
```

We need to use `-A:test` to bring this package into scope (on the classpath).

To resolve a package, invoke:

``` clojure
$ clj -A:test
Clojure 1.10.1
user=> (require '[cpm.api :as api])
nil
user=> (api/resolve-pkg {:package "babashka" :force true :verbose true})
;; output to stderr:
Downloading https://github.com/borkdude/babashka/releases/download/v0.2.1/babashka-0.2.1-macos-amd64.zip to /Users/borkdude/.cpm/packages/org/babashka/babashka/0.2.1/babashka-0.2.1-macos-amd64.zip
Unzipping /Users/borkdude/.cpm/packages/org/babashka/babashka/0.2.1/babashka-0.2.1-macos-amd64.zip to /Users/borkdude/.cpm/packages/org/babashka/babashka/0.2.1
Making /Users/borkdude/.cpm/packages/org/babashka/babashka/0.2.1/bb executable.
;; return value:
#object[java.io.File 0x29a98d9f "/Users/borkdude/.cpm/packages/org/babashka/babashka/0.2.1"]

user=> (api/resolve-pkg {:package "babashka" :verbose true})
;; return value:
#object[java.io.File 0x2a869a16 "/Users/borkdude/.cpm/packages/org/babashka/babashka/0.2.1"]
```

To create a path with packages (this implicitly resolves the packages like above):

``` clojure
$ clojure -M:test -m cpm.main babashka clj-kondo
/Users/borkdude/.cpm/packages/org/babashka/babashka/0.2.1:/Users/borkdude/.cpm/packages/clj-kondo/clj-kondo/2020.09.09
```

The resulting path can then be used to add programs on the path for the current shell:

``` clojure
$ export PATH=$(clojure -M:test -m cpm.main babashka clj-kondo):$PATH
$ which bb
/Users/borkdude/.cpm/packages/org/babashka/babashka/0.2.1/bb
$ which clj-kondo
/Users/borkdude/.cpm/packages/clj-kondo/clj-kondo/2020.09.09/clj-kondo
$ bb '(+ 1 2 3)'
6
```

CPM can also run with babashka for fast startup (requires version from master, check CircleCI artifacts):

``` clojure
$ bb -cp src:test-resources -m cpm.main clj-kondo
/Users/borkdude/.cpm/packages/clj-kondo/clj-kondo/2020.09.09
export PATH=$(bb -cp src:test-resources -m cpm.main clj-kondo):$PATH
$ which clj-kondo
/Users/borkdude/.cpm/packages/clj-kondo/clj-kondo/2020.09.09/clj-kondo
```

## TODO

- [ ] Make everything work with GraalVM for fast startup
- [ ] Support .tar.gz artifacts (currently only .zip)

## License

Copyright © 2019-2020 Michiel Borkent

Distributed under the EPL License. See LICENSE.
