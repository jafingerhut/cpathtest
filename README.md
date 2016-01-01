# cpathtest

A project purely for testing code that checks the classpath for which
namespaces are defined in each one, and which namespaces in one
directory of the classpath 'shadow' others.


## Steps followed to create the files described in the next section

The creation of `.class` files is not implemented right now.  These
instructions create the `.clj` and `.cljc` files as described in the
next section.

```clojure
(require '[cpathtest.core :as c])
(def cpaths (mapv c/canonical-filename [ "cp1" "cp2" ]))
(def suffs [ "clj" "cljc" ])

;; Create files to test on
(c/create-files cpaths suffs)
```


## Structure of files to test many cases of .class, .clj, and .cljc files defining the same namespace

There will be a separate Clojure namespace for each of the following
possibilities of whether they are defined in a `.class` file, a `.clj`
file, a `.cljc` file, or any subset of those.

For the first directory in the classpath, `cp1`, there are these
possibilities:

| `.class` | `.clj` | `.cljc` |
| -------- | ------ | ------- |
| N | N | N |
| N | N | Y |
| N | Y | N |
| N | Y | Y |
| Y | N | N |
| Y | N | Y |
| Y | Y | N |
| Y | Y | Y |

The same possibilities exist for `cp2`, the second directory in the
classpath, for a total number of combinations of 8 * 8 = 64.

For each of these possibilities, there will be one namespace defined
in that combination of ways.  The namespaces will be named in a way
that indicates in which files they are defined.

For example, namespace `foo.class00-clj10-cljc01` will have no
`.class` file in either `cp1` or `cp2` (the `00` after `class`
indicates no for both directories), it will have a `.clj` file in
`cp1` but not `cp2` (indicated by the `10` after `clj` in the name),
and it will have a `.cljc` file in `cp2` but not `cp1` (indicated by
the `01` after `cljc` in the name).

Each namespace will simply have an `ns` form with the namespace and
any `:require`s necessary, plus a single function `bar` that returns a
map containing:

* the key `:directory` with value `"cp1"` or `"cp2"`, indicating the
  directory that the file is in

* the key `:fname-suffix` with value `".class"`, `".clj"`, or
  `".cljc"` indicating the suffix of the file that the function is
  defined in.

* the key `:namespace` with value equal to the namespace as a symbol.
  This information should be redundant, but is a good double-check
  that the expected function has been called.

This will enable Clojure to require the namespace, call function `bar`
and from the return value to determine which file was loaded.


## Usage

Partial call tree for require, load, etc. in clojure.core

```
require
  (require and use both call load-libs directly)
use
  load-libs
    filter
    interleave
    complement
    remove
    libspec?
    prependss
    load-lib
      hash-map
      select-keys
      find-ns
      remove-ns
      (either calls load-all, or load-one directly)
      load-all
        commute
        reduce1
        sorted-set
        load-one
          root-resource
          load
            root-directory
            ns-name
            check-cyclic-dependency
            clojure.lang.RT/load
              classURL = getResource(baseLoader(), scriptbase + "__init.class");
              cljURL = getResource(baseLoader(), scriptbase + ".clj");
              if (cljURL == null) {
                  cljURL = getResource(baseLoader(), scriptbase + ".cljc");
              }
          find-ns
          commute
      ns-name
      alias
      refer
```

Is it possible to cause `tools.namespace` to give incorrect results
for namespace dependencies due to the following kind of situation?

Namespace `foo.bar` is defined in multiple files, either both a `.clj`
and a `.cljc` file, or perhaps also in multiple directories of the
classpath.  `tools.namespace` finds such a file that is _not_ the one
that would be loaded via `require`, and uses its dependencies, which
are different than the one that `require` would actually load.

Answer: Yes, it is possible for `tools.namespace` to give incorrect
dependencies back from a call to the `scan-dirs` function.  See
[TNS-42](http://dev.clojure.org/jira/browse/TNS-42).


## License

Copyright © 2015 Andy Fingerhut

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
