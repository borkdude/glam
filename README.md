# CPM

A package manager layered on top of the Clojure Tools.

Work in progress, not ready for production, breaking changes will happen.

## Usage

Place <package>.cpm.edn in your Clojure dependency.

E.g. from `test-resources`, `babashka.cpm.edn`:

``` clojure
{:package/name org.babashka/babashka
 :package/description ""
 :package/version "0.2.1"
 :package/license ""
 :package/artifacts
 [{:os/name "Mac.*"
   :os/arch "x86_64"
   :artifact/url "https://github.com/borkdude/babashka/releases/download/v0.2.1/babashka-0.2.1-macos-amd64.zip"}]}
```

We need to use `-A:test` to bring this package into scope.

To resolve that package, invoke:

``` clojure
$ clojure -A:test -X cpm.api/resolve-pkg :package babashka :force true :verbose true
Downloading https://github.com/borkdude/babashka/releases/download/v0.2.1/babashka-0.2.1-macos-amd64.zip to /Users/borkdude/.cpm/packages/org/babashka/babashka/0.2.1/babashka-0.2.1-macos-amd64.zip
Unzipping /Users/borkdude/.cpm/packages/org/babashka/babashka/0.2.1/babashka-0.2.1-macos-amd64.zip to /Users/borkdude/.cpm/packages/org/babashka/babashka/0.2.1
```

To create a path with the resolved binary (this implicitly does the above):

``` clojure
$ clojure -A:test -M -m cpm.main babashka
/Users/borkdude/.cpm/packages/org/babashka/babashka/0.2.1
```

The resulting path can then be used to add programs on the path for the current shell:

``` clojure
$ export PATH=$(clojure -A:test -M -m cpm.main babashka):$PATH
$ which bb
/Users/borkdude/.cpm/packages/org/babashka/babashka/0.2.1/bb
$ bb '(+ 1 2 3)'
6
```

## TODO

- [ ] Make everything work with GraalVM for fast startup
- [ ] Support .tar.gz artifacts (currently only .zip)

## License

Copyright © 2019-2020 Michiel Borkent

Distributed under the EPL License. See LICENSE.
