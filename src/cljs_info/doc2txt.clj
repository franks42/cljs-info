(ns cljs-info.doc2txt
  "Namespace dedicated to the generation of text-formatted documentation for
  vars, namespaces and types in ClojureScript."
  (:require [clojure.set]
            [clojure.string]
            [cljs-info.ns]
            [cljs.analyzer]))

;;;;

(def cljs-special-forms-doc
  "Some cljs-specific forms are no real vars and have no online-docs.
   This map substitutes for those."
  {
  'load-namespace {:name 'load-namespace :arglists '(quote ([n])) :doc "Load a namespace and all of its dependencies into the evaluation environment.
  The environment is responsible for ensuring that each namespace is loaded once and
  only once." :special-form true}
  'load-file {:name 'load-namespace :arglists '(quote ([n])) :doc "load-file." :special-form true}
  'in-ns {:name 'load-namespace :arglists '(quote ([n])) :doc "Sets *cljs-ns* to the namespace named by the symbol n, creating it if needed." :special-form true}
  })
 
;; text-docs generation

(defn doc2txt
  "Generates and returns string with text-page for the docs-map info obtained for identifier-string w."
  ([w] (doc2txt (cljs.analyzer/empty-env) w))
  ([env w]
    (let [s (symbol w)
         
          m (or (and (cljs-info.ns/cljs-find-ns s) (@cljs.analyzer/namespaces s))
                (cljs-special-forms-doc s)
                (cljs.analyzer/resolve-existing-var env s))

          fqname (:name m)
          the-name (name fqname)
         
          title (if m
                  (str  (:name m)
                        "   -   "
                        (when (:private m) "Private ")
                        (cond
                          (cljs-info.ns/cljs-find-ns s)  "Namespace"
                          (:fn-var m)               "Function"
                          (:special-form m)         "Special Form"
                          (:protocol-symbol m)      "Protocol"
                          (:type m)                 "Type"
                          :true                     "Var"))
                  (str "Sorry, no doc-info for \"" w "\""))
 
          message (if m
            (str
             
              (when (:protocol m)
                (str \newline "Protocol: " (:protocol m)))
             
              (when (:protocols m)
                (str \newline "Protocols: " (:protocols m)))
             
              (when (:arglists m)
                (str  \newline
                  (if (string? (:arglists m))
                    (:arglists m)
                    (eval (:arglists m)))))
 
              (when (:doc m)
                (str  ;\newline "Documentation:"
                      \newline "  " (:doc m)))
 
            ""))]
      {:title title :message message})))

