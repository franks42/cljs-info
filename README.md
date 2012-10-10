cljs-info
=========

"cljs-info" is collection of modules to provide basic help and reflection facilities for ClojureScript, like doc, apropos, source, ns-map &amp friends.

## Introduction

When you work with lein-cljsbuild and the cljs-repl, then you will have two virtual clojurescript worlds: one reflected on the clojure-side and the other on the javascript side. In other words, there is no "real" ClojureScript environment, which is a mental model you will have to get used to.

The cljs-code is translated into javascript, which is downloaded to and evaluated on the js-vm. For the compilation process, the clojure-instance running on the jvm, maintains all kinds of meta and mappings info about the compiled cljs, which is used for any subsequent compilation of cljs-forms.

Using the cljs-repl, you can change and introspect the state of the javascript world by submitting cljs-forms and loading cljs-files/namespaces. To help you in the process, ideally you would like similar  facilities as those you're used to while developing Clojure-code, like online docs, apropos, all-ns, ns-resolve, source, ns-map/publics/refers/etc. Unfortunately, the existing clj-facilities can not be used for ClojureScript because of the split-personality: the meta-data of all ClojureScript functions and variable are maintained on the clj-side in the jvm, while their instances live as compiled javascript in the browser's js-vm.

So... all the functions of this cljs-info module that provides access to cljs' namespace and variable meta-data, run on the clj-side.

### Two REPL operation.

One mode of operation is to have two repls running on the same jvm: one cljs-repl and a clj-repl. The cljs-repl is used to submit cljs-forms that are evaluated in the browser. The clj-repl will allow you to introspect the cljs-meta world as it is maintained on the clj-side. For example, if you like to see the docstring of the cljs-function "my-cljs-fn", then you would submit "(cljs-doc my-cljs-fn)" in the clj-repl. An other example is that define a new cljs-function in the cljs-repl, after which you can see its docstring and resolution properties in the clj-repl.

### Single REPL operation.

The two repls are a bit inconvenient, and we can hide the fact that we have to retrieve the cljs-meta data from the clj-side by transparently communicating between the cljs-js side and the clj-jvm. In other words, we would have a cljs doc-function that is a proxy which will make an rpc-like call to the clj-jvm to evaluate the before-mentioned cljs-doc function and download the result. The advantage of this approach is that you would only have a single repl to work with, and that you can use the results of the help and reflection functions directly in your cljs-code.

Unfortunately, the reflection facilities are a work in progress and those rpc-like proxies are being worked on... Currently there is a cljs-function "cljs.reflect/doc" in the clojurescript repo that works a little bit in certain setups - hopefully its improved version will show us the way how to communicate properly between the cljs-js and the clj-jvm.  

### One and a half REPL operation.

If you really want to work in a single cljs-repl "now", then there is a backdoor facility available that allows you to execute clj-functions on the jvm from the cljs-repl. One could argue that this facility is an ugly, nasty hack... as it kind of makes you believe those functions are executing in the cljs-context, but they are not because those are not real cljs-functions, do not return anything and can only communicate back by printing to the jvm's stdout. 

Even though the cljs-info module makes some of the help and ns-info functions available thru this hack, it should be seen as a temporary solution that should be burnt and destroyed as soon as a solid rpc-like solution is available.


# cljs-info.doc

The cljs-info.doc namespace provides the cljs-doc macro and cljs-doc* function, which are equivalent to   clj's venerable doc macro, except that cljs-doc knows how to find the docstrings from cljs' namespaces, functions, macros and variables. (it's functionality is similar to cljs.reflect/doc except that it works in all setups and it arguably provides more info...) 

# cljs-info.ns

The cljs-info.ns namespace provides equivalent implementations for cljs of clj's all-ns, ns-resolve, find-ns, apropos, source, ns-map, ns-publics, etc. The cljs-equivalent functions have the same names prepended by "cljs-".

It is good to remember that ClojureScript doesn't have any "var" datatype and that the symbols are essentially directly mapped to the js-variables/objects. Also there is no special namespace type and a ns in cljs is identified by a symbol. Those differences are reflected in the cljs-functions like for example cljs-find-ns will return the symbol for the found namespace instead of a namespace-object, and cljs-ns-resolve will return a fqname as a symbol instead of a var.

# cljs-info.repl

The cljs-info.repl namespace provides a number of functions to start and to interact with the cljs-repl. 

### run-repl-listen

None of the cljs-info.repl functions are essential for the cljs-info.doc and cljs-info.ns related functions, but if you want to work in the "one and a half repl" mode, you will have to start the cljs-repl with certain configuration parameters. The "run-repl-listen" function is a plug-in replacement for the equivalent lein-cljsbuild one. The difference is that it configures a number of the cljs-doc and cljs-ns-* functions such that they can be called from the cljs-repl. Again... please reread the "one and a half repl" section to understand the caveats.

### cljs->repl, cljs->repl* and js->repl

The cljs->repl macro and accompanying cljs->repl* function, allow you to submit cljs-forms from your clj-environment (clj-code or clj-repl) for compilation and eval in the browser. Submitting cljs-forms thru these functions is the equivalent of typing them in the cljs-repl by hand. The ability to programmatically send cljs-forms to or thru the repl, gives you the tool to, for example, select a cljs-statement in an email message, use macosx's automation to send that form to the jvm, and subsequently compile and eval in your browser's js-vm...

The js->repl function is the equivalent of cljs->repl* for javascript-code. It takes a string of js-code and sends it to the browser for eval over the cljs-repl connection, and returns the result back to the caller. 

In all cases for cljs->repl, cljs->repl* and js->repl, the result from the eval in the js-vm is communicated back to the caller. The site-effect operations that print to *out*, however, will send the output by default to the repl-terminal's stdout. In some cases that is good enough, but in most cases you want that output to be send to the stdout of the caller's context. In order to achieve that, we have to jump through some hoops, because the normal (binding [*out* ...]...) doesn't work. The "printing" from the js-vm is communicated back thru a separate post-request and handled completely asynchronous from the initial calling thread. I'm not really happy with the current solution that relies on a storing the caller's *out* value in a single global atom that is used by a patched version of clojurescript's post-handler - it works but I'm eagerly awaiting suggestions for more elegant solutions.

The set-local-printing! function determines whether you want any subsequent printed output to go the the repl-terminal (false) or to the caller's stdout (true).


## Meta-data description for the ClojureScript namespaces, vars and resolution

@cljs.analyzer/namespaces maintains the meta-data of the namespaces and vars defined within your cljs-environment.

It is a map of namespace-name to namespace-meta-data: {namespace1 namespace-metadata1, namespace2 namespace-metadata2}
therefor (keys @cljs.analyzer/namespaces) gives you a set of all cljs-namespaces as a seq of symbols.

The namespace's meta-data value is also a map, with keys like :defs, :imports, :requires-macros, :uses-macros, :requires, :uses, :excludes, and :name.

The :defs entry of a namespace is a map for all the vars defined in that namespace. The keys for the entries are the var names (without the namespace component), while the associated value is a map holding the metadata for that var. 

The var's metadata map has properties like: :arglists, :method-params, :name, :protocol-impl, :max-fixed-arity, :protocol-inline, :variadic, :line, :fn-var, :file.



## License

Copyright (C) 2012 Frank Siebenlist

Distributed under the Eclipse Public License, the same as Clojure.
