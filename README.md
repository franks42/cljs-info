cljs-info
=========

"cljs-info" is collection of clojure-functions to provide basic help and reflection facilities for ClojureScript, like doc, apropos, source, ns-map &amp friends.

## Installation & Basic Usage

You should really read the "What, Why, How..." section first, but most of you will skip to the install section anyway so I moved it upfront.

You need a single dependency-entry in your Leiningen's project.clj:

    :dependencies [... 
                    [cljs-info "1.0.0"] 
                  ...] 

After which you can choose between a 2-repl or one-and-a-half-repl setup (read the "What, Why, How..." section!).

### 2-REPL SETUP

In one terminal session, you run the cljs-repl as you normally would.

While in the second session on the same repl-jvm you would run a clj-repl, where you can concurrently query the live ClojureScript world about its innards:

    user=> (use 'cljs-info.doc) 
    nil 
    user=> (use 'cljs-info.ns) 
    nil 
    user=> (doc jayq.util/clj->js)
    nil
    user=> (cljs-doc jayq.util/clj->js)
    ----------------------------------------------------------------------
    jayq.util/clj->js   -   Function 
    ([x])
      Recursively transforms ClojureScript maps into Javascript objects,
       other ClojureScript colls into JavaScript arrays, and ClojureScript
       keywords into JavaScript strings.
    
    user=> (cljs-all-ns) 
    #{cljs.core cljs.user clojure.browser.event clojure.browser.net clojure.browser.repl 
    clojure.browser.repl.client clojure.reflect clojure.string example.crossover.shared 
    example.hello example.repl jayq.core jayq.util jquery-test myreflect} 
    user=>  
    

### ONE-AND-A-HALF-REPL SETUP

In this case you will be able to use most of the doc and ns-* facilities straight from the cljs-repl, but it uses a temporary hack that we really shouldn't be using (read the "What, Why, How..." section).

We first start a clj-repl session and start the cljs-repl with the customized "cljs-info.repl/run-repl-listen" function, after which some of the reflection facilities are available:

    user=> (require 'cljs-info.repl) 
    nil 
    user=> (cljs-info.repl/run-repl-listen) 
    "Type: " :cljs/quit " to quit" 
    ClojureScript:cljs.user> (cljs-doc js->clj)
    ----------------------------------------------------------------------
    cljs.core/js->clj   -   Function 
    ([x & options])
      Recursively transforms JavaScript arrays into ClojureScript
      vectors, and JavaScript objects into ClojureScript maps.  With
      option ':keywordize-keys true' will convert object fields from
      strings to keywords.
    
    ClojureScript:cljs.user> (cljs-ns-resolve replace) 
    cljs.core/replace 
    ClojureScript:cljs.user> 


### cljs->repl

When you started the cljs-repl with the customized "cljs-info.repl/run-repl-listen", then you can programatically send cljs-forms for compilation&eval with "cljs-info.repl/cljs->repl".

You can test it out by starting yet another clj-repl:

    user=> (use 'cljs-info.repl) 
    nil 
    user=> (cljs->repl (do (println "yes it works - stdout comes here...")(js/alert "and it runs in the browser") 42)) 
    yes it works - stdout comes here... 
    "42" 
    user=> 
    
That's enough to get you started - now please read the next section.

## What, Why, How...

When you work with lein-cljsbuild and the cljs-repl, then you will have two virtual clojurescript worlds: one reflected on the clojure-side as ASTs and metadata-maps, and the other on the javascript side as native js-object and functions. In other words, there is no "real" ClojureScript environment, which is a mental model you will have to get used to.

The cljs-code is translated into javascript, which is downloaded to and evaluated on the js-vm. For the compilation process, the clojure-instance running on the jvm, maintains all kinds of meta and mappings info about the compiled cljs, which is used for any subsequent compilation of cljs-forms. When you use the compiler in a stand-alone mode, this compilation process is a one-shot deal, and all the clojurescript metadata only exists during the comiplation phase and is not available before or after.

When you work with the cljs-repl, however, the clj-side maintains the cljs-metadata for as long as the repl-jvm is alive, and updates the metadata state with every new cljs-compilation. The cljs-repl allows you to change the state of the cljs-world and its associated javascript world by submitting cljs-forms and loading cljs-files/namespaces. By running a second clj-repl on that same jvm-instance, you can introspect the cljs-metadata.

Just to reiterate the split-personalities that are present during development with lein-cljsbuild, you have to realize that three (3) different jvm-instances are used that do not share any live state. Plus a separate js-vm if you find the mental picture not complicated enough. One cljs-compiler-jvm is used for the automatic compilation of cljs-code which makes one-shot passes that spit out js-code in a server-based directory. Another webserver-jvm is dedicated to serve webpages and that generated js-code, but knows nothing of the ClojureScript that generated the js-code is serves. Then we have the cljs-repl jvm-server, that maintains a live cljs-metadata model of the code that it loads from the cljs-source code files and the cljs-forms it compiles at the repl. Note that the cljs-compiler-jvm and the repl-jvm do not share any state. Lastly, we have the js-vm that loads code from the webserver that was statically compiled by the cljs-compiler server, and it loads code dynamically that comes from the compiled cljs-forms from the repl-server. (we need a picture here to drive that message home...)

As a consequence of the described cljs-developement setup, you can only introspect the live cljs-model thru the repl-server while it is connected to the browser's js-vm, because that is the only server that maintains the cljs-metadata model "persistently". 

To help you in the repl-aided development process, ideally you would like similar reflection-facilities as those you're used to while developing Clojure-code, like online docs, apropos, all-ns, ns-resolve, source, ns-map/publics/refers/etc. Unfortunately, the existing clj-facilities can not be used for ClojureScript because its metadata is stored in different places. As a result, the reflection-facilities for ClojureScript had to be rewitten, and that's essentially what this project is about.

So...after this expose, it is (hopefully) clear that all the functions of this cljs-info module that provides views of the cljs' namespaces', variables' and resolution's meta-data, run on the clj-side, are complete rewrites of their clj counterparts, and only have access to the cljs-metadata on the live repl-jvm connected to the browser's js-vm.

### Two REPL operation.

One mode of operation is to have two repls running on the same jvm: one cljs-repl and a clj-repl. The cljs-repl is used to submit cljs-forms that are evaluated in the browser. The clj-repl will allow you to introspect the cljs-meta world as it is maintained on the clj-side. For example, if you like to see the docstring of the cljs-function "my-cljs-fn", then you would submit "(cljs-doc my-cljs-fn)" in the clj-repl. An other example is to define a new cljs-function in the cljs-repl, after which you can query its docstring and resolution properties in the clj-repl.

### Single REPL operation.

The two repls are a bit inconvenient, and we can hide the fact that we have to retrieve the cljs-meta data from the clj-side by transparently communicating between the cljs-js side and the clj-jvm. In other words, we would have a cljs doc-function that is a js-compiled proxy which will make an rpc-like call to the clj-jvm to evaluate the before-mentioned cljs-doc function and download the result. The advantage of this approach is that you would only have a single repl to work with, and that you can use the results of the help and reflection functions directly in your cljs-code.

Unfortunately, the currently available reflection facilities (clojure.reflect) are a work in progress and those rpc-like proxies are being worked on... Currently there is a cljs-function "cljs.reflect/doc" in the clojurescript repo that works a little bit in certain setups - hopefully its improved version will soon show us the proper way to communicate  between the cljs-js-vm and the clj-jvm.  

### One and a half REPL operation.

If you really want to work in a single cljs-repl "now", then there is a backdoor facility available that allows you to execute clj-functions on the jvm from the cljs-repl. One could argue that this facility is an ugly, nasty hack... as it kind of makes you believe those functions are executing in the cljs-context, but they are not because those are not real cljs-functions, do not return anything and can only communicate back by printing to the cljs-repl-jvm's stdout. Note that currently, the load-namespace, load-file and in-ns commands are implemented that way in the clojurescript distro. There is "wide-consensus" that this is not a very clojuresque solution.

Even though the cljs-info module makes some of the help and ns-info functions available thru this hack, it should be seen as a temporary solution that should be burnt, destroyed and forgotten as soon as a solid rpc-like solution is available.


# cljs-info.doc

The cljs-info.doc namespace provides the "cljs-doc macro" and "cljs-doc*" function, which are equivalent to   clj's venerable "doc" macro, except that cljs-doc knows how to find the docstrings for cljs' namespaces, functions, macros and variables. (it's functionality is similar to cljs.reflect/doc except that it works in all setups and it arguably provides more info...) 

# cljs-info.ns

The cljs-info.ns namespace provides equivalent implementations for cljs of clj's all-ns, ns-resolve, find-ns, apropos, source, ns-map, ns-publics, etc. Note that the cljs-equivalent functions have the same names prepended by "cljs-".

It is good to remember that ClojureScript doesn't have any "var" datatype and that the symbols are essentially directly mapped to the js-variables/objects. Also there is no special namespace type and a ns in cljs is identified by a symbol. Those differences are reflected in the cljs-functions like for example cljs-find-ns will return the symbol for the found namespace instead of a namespace-object, and cljs-ns-resolve will return a fqname as a symbol instead of a var.

# cljs-info.repl

The cljs-info.repl namespace provides a number of functions to start and to interact with the cljs-repl. 

### run-repl-listen

None of the cljs-info.repl functions are essential for the cljs-info.doc and cljs-info.ns related functions, but if you want to work in the "one and a half repl" mode, you will have to start the cljs-repl with certain configuration parameters. The "run-repl-listen" function is a plug-in replacement for the equivalent lein-cljsbuild function with the same name. The difference is that "cljs-info.repl/run-repl-listen" configures proxies for a number of the cljs-doc and cljs-ns-* functions such that they can be called from the cljs-repl. Again... please reread the "one and a half repl" section to understand the caveats.

Note that "cljs->repl" and "cljs->repl*" also rely on the special configuration setting and therefor also require the cljs-repl to be started with "cljs-info.repl/run-repl-listen".

### cljs->repl, cljs->repl* and js->repl

The cljs->repl macro and accompanying cljs->repl* function, allow you to submit cljs-forms from your clj-environment (clj-code or clj-repl) for compilation and eval in the browser. Submitting cljs-forms thru these functions is the equivalent of typing them in the cljs-repl by hand. The ability to programmatically send cljs-forms to or thru the repl, gives you the tool to, for example, select a cljs-statement in an email message, use macosx's automation to send that form to the jvm, and subsequently compile and eval in your browser's js-vm...

The js->repl function is the equivalent of cljs->repl* for javascript-code. It takes a string of js-code and sends it to the browser for eval over the cljs-repl connection, and returns the result back to the caller. It gives you an easy way to introspect the js-vm and call any js-function.

In all cases for cljs->repl, cljs->repl* and js->repl, the result from the eval in the js-vm is communicated back to the caller. The site-effect operations that print to \*out\*, however, will send the output by default to the repl-terminal's stdout. In some cases that is good enough, but in most cases you want that output to be send to the stdout of the caller's context. In order to achieve that, we have to jump through some hoops, because the normal (binding [\*out\* ...]...) doesn't work. The "printing" from the js-vm is communicated back thru a separate post-request and handled completely asynchronous from the initial calling thread. I'm not really happy with the current solution that relies on a storing the caller's \*out\* value in a single global atom that is used by a patched version of clojurescript's post-handler - it works but I'm eagerly awaiting suggestions for more elegant solutions.

The set-local-printing! function determines whether you want any subsequent printed output to go the the repl-terminal (false) or to the caller's stdout (true).


## Meta-data description for the ClojureScript namespaces, vars and resolution

@cljs.analyzer/namespaces maintains the meta-data of the namespaces and vars defined within your cljs-environment.

It is a map of namespace-name to namespace-meta-data: {namespace1 namespace-metadata1, namespace2 namespace-metadata2}
therefor (keys @cljs.analyzer/namespaces) gives you a set of all cljs-namespaces as a seq of symbols.

The namespace's meta-data value is also a map, with keys like :defs, :imports, :requires-macros, :uses-macros, :requires, :uses, :excludes, and :name.

The :defs entry of a namespace is a map for all the vars defined in that namespace. The keys for the entries are the var names (without the namespace component), while the associated value is a map holding the metadata for that var. 

The var's metadata map has properties like: :arglists, :method-params, :name, :protocol-impl, :max-fixed-arity, :protocol-inline, :variadic, :line, :fn-var, :file.

## Clj-ns-browser for ClojureScript?

Working in progress... patience ;-)

https://raw.github.com/franks42/clj-ns-browser/cljs/Cljs%20Browser%202012-10-05.png

## License

Copyright (C) 2012 Frank Siebenlist

Distributed under the Eclipse Public License, the same as Clojure.
