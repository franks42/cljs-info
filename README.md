cljs-info
=========

Doc, apropos, ns-map &amp friends for clojurescript# cljs-info


## ClojureScript namespaces, vars and resolution


@cljs.analyzer/namespaces maintains the mata-data of the namespaces and vars defined within your cljs-environment.

It is a map of namespace-name to namespace-meta-data: {namespace1 namespace-metadata1, namespace2 namespace-metadata2}
therefor (keys @cljs.analyzer/namespaces) gives you a set of all cljs-namespaces as a seq of symbols.
and (vals  @cljs.analyzer/namespaces) is the list of all the associated namespace meta-data,
which is also a set as values are uniquely defined per namespace as we will see.

The namespace's meta-data value is also a map, with keys like :defs, :imports, :requires-macros, :uses-macros, 
:requires, :uses, :excludes, and :name. 
The last :name uniquely identifies the namespace's meta-data map, 
and its symbol value is identical to the namespace-key of the outer-map @cljs.analyzer/namespaces.
note that within cljs the namespace is identified with a symbol and there is no real namespace object as in clj.



## License

Copyright (C) 2012 Frank Siebenlist

Distributed under the Eclipse Public License, the same as Clojure.
