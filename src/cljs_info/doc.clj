;; Copyright (c) Frank Siebenlist. All rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file COPYING at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns cljs-info.doc
  "Namespace dedicated to the generation of text-formatted documentation for
  vars, namespaces and types in ClojureScript."
  (:require [cljs-info.doc2txt]
            [clj-info]
            [clj-info.doc2txt]
            [cljs-info.ns]
            [cljs.analyzer]))


(defn cljs-doc*
  "Function prints documentation for a ClojureScript variable, namespace,
  or special form.  Name n is string or quoted symbol."
  ([] (clj-info/tdoc* 'cljs-info.doc/cljs-doc*))
  ([n]
    (let [env (cljs.analyzer/empty-env)
          s (symbol (str (if (= (type n) clojure.lang.Cons) (second n) n)))
          n-maybe-cljs-core (if (namespace s) s (symbol (str "cljs.core/" s)))
          m (if (or (cljs-info.ns/cljs-find-ns s)
                    (cljs-info.ns/cljs-ns-resolve cljs.analyzer/*cljs-ns* s)
                    (cljs-info.doc2txt/cljs-special-forms-doc s))
              (cljs-info.doc2txt/doc2txt env s)
              (if (ns-resolve *ns* s)
                (clj-info.doc2txt/doc2txt s)
                (if (ns-resolve *ns* n-maybe-cljs-core)
                  (clj-info.doc2txt/doc2txt n-maybe-cljs-core)
                  {:title (str "Sorry, no doc-info for \"" s "\"")})))]
      (println "----------------------------------------------------------------------")
      (println (:title m) (:message m)))
    (symbol "")) ;; suppresses the eval-print of nil - purely esthetics
  ([n & m-n]
    ;; allows for (apply cljs-doc* (apropos "replace"))
    (doall (map cljs-doc* (cons n m-n))) (symbol "")))


(defmacro cljs-doc
  "Macro prints documentation for a ClojureScript variable, namespace,
  or special form. Name n is string, symbol, or quoted symbol."
  ([] `(clj-info/tdoc* "cljs-doc"))
  ([n]
    (cond (string? n) `(cljs-doc* ~n)
      (symbol? n) `(cljs-doc* ~(str n))
      (= (type n) clojure.lang.Cons) `(cljs-doc* ~(str (second n))))))


(defn cljs-find-doc
  "Prints documentation for any cljs-variable/fn/ns whose documentation or name
 contains a match for re-string-or-pattern"
  [str-or-pattern]
  (doseq [fqn (cljs-info.ns/cljs-apropos-doc str-or-pattern)] 
    (println (cljs-doc* fqn)))
  (symbol ""))

