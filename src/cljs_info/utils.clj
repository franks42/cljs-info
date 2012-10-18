;; Copyright (c) Frank Siebenlist. All rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file COPYING at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns cljs-info.utils
  "ClojureScript utility functions for ns & meta stuff."
  (:require [clojure.set]
            [cljs.analyzer :as ana])
  (:use [cljs-info.ns]
        [clj-info.utils]))

(declare cljs-fqname)
(declare cljs-fqname-sym)

;;;;
;; predicates hiding impl specifics

;; basic type-predicates
;; note that we already have char? class? coll? decimal? empty? fn? ifn? future? keyword? list?
;; map? nil? number? seq? sequential? set? special-symbol? string? symbol? var? vector?


(defn fqn->ns-name
  "For a name n of an existing var or namespace, returns [a-ns a-name],
  or [a-ns nil], or nil, depending on n." 
  [n]
  (when-let [fqn (cljs-fqname-sym n)]
    (if (cljs-namespace? fqn)
      [(str fqn) nil]
      [(namespace fqn) (name fqn)])))

(defn fqn->ns-name-sym
  "For a name n of an existing var or namespace, returns [a-ns a-name],
  or [a-ns nil], or nil, depending on n." 
  [n]
  (when-let [fqn (cljs-fqname-sym n)]
    (if (cljs-namespace? fqn)
      [fqn nil]
      [(symbol (namespace fqn)) (symbol (name fqn))])))

(defn ns-name-map-sym
  "For a name, var or ns of n of an existing var or namespace, returns {:ns a-ns :name a-name},
  or {:ns a-ns}, or nil, depending on n.
  BAD as it shadows the functions ns and name after destructuring... need better names" 
  [n]
  (when-let [fqn (cljs-fqname-sym n)]
    (if (cljs-namespace? fqn)
      {:ns fqn}
      {:ns (symbol (namespace fqn)) :name (symbol (name fqn))})))


(defn cljs-meta 
  "Returns the @namespaces metadata map for the given ns or var." 
  [ns-or-var]
  (when-let [[a-ns a-name] (fqn->ns-name-sym ns-or-var)]
    (if a-name
      (get-in @cljs.analyzer/namespaces [a-ns :defs a-name])
      (get-in @cljs.analyzer/namespaces [a-ns]))))
      



(defn fn-var? 
  "" 
  [n] 
  (when-let [[a-ns a-name] (fqn->ns-name-sym n)]
    (when a-name
      (get-in @cljs.analyzer/namespaces [a-ns :defs a-name :fn-var]))))

(defn fn-var2? 
  "" 
  [n] 
  (when-let [{:keys [ns# name#]} (ns-name-map-sym n)]
    (println ns# name#)
    (when name#
      (get-in @cljs.analyzer/namespaces [ns# :defs name# :fn-var]))))
      

;; (defn cljs-special-form?
;;   "Predicate that returns true if given name n (string or symbol) is a special-form and false otherwise.
;;   Note that some special forms are vars/macros, and some are special-symbols.
;;   All the none-vars are tested by clojure.core/special-symbol?"
;;   [n]
;;   (if (and (var? n) (:special-form (meta n)))
;;     true
;;     (when-let [n-str (and n (str n))]
;;       (or (some #(= % n-str) special-forms)
;;         (when-let [v (resolve-fqname n-str)]
;;           (and (var? v) (:special-form (meta v))))))))
;; 
;; 
;; (defn cljs-macro?
;;   "Predicate that returns true when var v is a macro, and false otherwise.
;;   Note that input is a var - if you want to input a name-string or -symbol, use:
;;   (macro? (resolve-fqname n))"
;;   [v]
;;   (and (var? v) (:macro (meta v))))
;; 
;; 
;; (defn cljs-atom?
;;   "Predicate that returns true when var v refers to a atom, and false otherwise.
;;   Note that input is a var - if you want to input a name-string or -symbol, use:
;;   (atom? (resolve-fqname n))"
;;   [v]
;;   (isa? (type v) clojure.lang.Atom))
;; 
;; 
;; (defn cljs-multimethod?
;;   "Predicate that returns true when var v refers to a multi-method, and false otherwise.
;;   Note that input is a var - if you want to input a name-string or -symbol, use:
;;   (multimethod? (resolve-fqname n))"
;;   [o]
;;   (isa? (type o) clojure.lang.MultiFn))
;; 
;; 
;; (defn cljs-var-multimethod?
;;   "Predicate that returns true when var v refers to a multi-method, and false otherwise.
;;   Note that input is a var - if you want to input a name-string or -symbol, use:
;;   (multimethod? (resolve-fqname n))"
;;   [v]
;;   (and (var? v) (multimethod? @v)))
;; 
;; 
;; (defn cljs-protocol?
;;   "Predicate that returns true when var v refers to a protocol, and false otherwise.
;;   Note that input is a var - if you want to input a name-string or -symbol, use:
;;   (protocol? (resolve-fqname n))"
;;   [v]
;;   (and (var? v) (isa? (type @v) clojure.lang.PersistentArrayMap) (:on-interface @v) true))
;; 
;; 
;; (defn cljs-deftype?
;;   "Predicate that returns true when object o refers to a deftype, and false otherwise."
;;   [o]
;;   (isa? o clojure.lang.IType))
;; 
;; 
;; (defn cljs-defrecord?
;;   "Predicate that returns true when object o refers to a defrecord, and false otherwise."
;;   [o]
;;   (isa? o clojure.lang.IRecord))
;; 
;; 
;; (defn cljs-protocol-fn?
;;   "Predicate that returns true when var v refers to a protocol function, and false otherwise.
;;   Note that input is a var - if you want to input a name-string or -symbol, use:
;;   (protocol-fn? (resolve-fqname n))"
;;   [v]
;;   (if (and (var? v) (:protocol (meta v)) true) true false))
;; 
;; 
;; (defn cljs-defn?
;;   "Predicate that returns true when var v is a function with a specified arglist, and false otherwise.
;;   Note that input is a var - if you want to input a name-string or -symbol, use:
;;   (function? (resolve-fqname n))"
;;   [v]
;;   (if-let [m (and (var? v) (meta v))]
;;     (if (and (not (or (:macro m)(:special-form m))) (fn? @v) (:arglists m))
;;       true
;;       false)
;;     false))
;; 
;; 
;; (defn cljs-namespace?
;;   "Predicate that returns true when n refers to a namespace, and false otherwise.
;;   Note that input is a name referring to a namespace - if you want to input a name-string or -symbol, use:
;;   (namespace? (resolve-fqname n))"
;;   [maybe-ns]
;;   (isa? (type maybe-ns) clojure.lang.Namespace))
;; 
;; 
;; ;; Two convenience function for clojure.tools.trace
;; ;; that should ideally be part of that library
;; 
;; (defn cljs-var-traceable? 
;;   "Predicate that returns whether a var is traceable or not."
;;   [v]
;;   (let [vv (or (and (var? v) v) (and (symbol? v) (resolve v)))]
;;     (and (var? vv) (ifn? @vv) (-> vv meta :macro not))))
;; 
;; 
;; (defn cljs-dynamic?
;;   "Predicate that returns whether a var is dynamic."
;;   [v] 
;;   (let [vv (or (and (var? v) v) (and (symbol? v) (resolve v)))]
;;     (and (var? vv) (meta vv) (:dynamic (meta vv)))))
;; 
;; 
;; (defn cljs-var-traced?
;;   "Predicate that returns whether a var is currently being traced.
;;   (should ideally be part of clojure.tools.trace such that we can
;;   remain oblivious about the trace-implementation internals)"  
;;   ([ns s]
;;      (var-traced? (ns-resolve ns s)))
;;   ([v]
;;     (let [vv (or (and (var? v) v) (and (symbol? v) (resolve v)))]
;;        (and (var? vv) (meta vv) ((meta vv) ::clojure.tools.trace/traced)))))



;;;;
;; fqname

(defprotocol IFQCljsNameable
  "Protocol to dispatch on the fqname by type."
  (-cljs-fqname [o] [o n] 
  "Returns a string with the fully qualified name of an existing object 
  that is either o or o will resolve to it.
  The optional namespace n may be used for resolution - defaults to cljs.analyzer/*cljs-ns*.
  Returns nil when no real object exists or o cannot resolve to real object."))

(extend-type java.lang.Object
  ;; default value nil for all types that are not IFQCljsNameable
  IFQCljsNameable
  (-cljs-fqname ([o] nil)
          ([o n] nil)))

(extend-type clojure.lang.Var
  IFQCljsNameable
  (-cljs-fqname ([o] (subs (str o) 2))
          ([o _] (-cljs-fqname o))))

(extend-type clojure.lang.Symbol
  IFQCljsNameable
  (-cljs-fqname ([o] (-cljs-fqname o ana/*cljs-ns*))
                ([o n] 
                  (if-let [a-ns (cljs-find-ns o)] 
                     (str a-ns)
                     (if-let [v (cljs-ns-resolve n o)] 
                       (str v)
                       nil)))))

(extend-type clojure.lang.Keyword
  IFQCljsNameable
  (-cljs-fqname ([o] (if-let [n (namespace o)] (str n "/" (name o)) (name o)))
          ([o n] (-cljs-fqname o))))

(extend-type java.lang.String
  IFQCljsNameable
  (-cljs-fqname ([o] (-cljs-fqname (symbol o)))
          ([o n] (-cljs-fqname (symbol o) n))))

(defn cljs-fqname 
  "fqname returns the fully qualified string-name of existing cljs-object o,
  or returns the fqn of the existing object that o resolves to.
  Returns nil if no fqn is found or applicable."
  ([o] (-cljs-fqname o))
  ([n o] (-cljs-fqname o n)))

(defn cljs-fqname-sym
  "fqname returns the fully qualified symbol-name of existing cljs-object o,
  or returns the fqn of the object o resolves to.
  Returns nil if no fqn is found or applicable."
  ([o] (when-let [s (cljs-fqname o)] (symbol s)))
  ([n o] (when-let [s (cljs-fqname n o)] (symbol s))))


