;; Copyright (c) Frank Siebenlist. All rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file COPYING at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns cljs-info.ns
  "Clojure-library for the ClojureScript resolution environment that gives
  the equivalent functionality of some of the namespace specific resolution
  and info functions, like all-ns, ns-map, ns-resolve, apropos, etc.
  The naming convention used is to prepend the clojure-equivalent function-names
  with cljs- , such that ns-map becomes cljs-ns-map.
  Note that this is not a clojurescript-library, but its functions can be called
  from the cljs-repl with some limitations."
  (:require [clojure.set]
            [cljs.analyzer :as ana]))

;;;;

(defn cljs-all-ns
  "Returns a sequence of all cljs-namespaces as symbols."
  [] (apply sorted-set (keys @ana/namespaces)))

(defn cljs-all-ns-str
  "Returns a sequence of all cljs-namespaces as strings."
  [] (map str (cljs-all-ns)))

(defn cljs-find-ns
  "Returns the cljs-namespace as a symbol for the given symbol or string,
  or nil if no cljs-namespace can be found."
  [n]
  (let [s (symbol n)] (when (@ana/namespaces s) s)))

(defn cljs-the-ns
  "When passed a symbol or string, returns the cljs-namespace named by it,
  throwing an exception if not found."
  [n]
  (or (cljs-find-ns n)
      (throw (Exception. (str "No cljs-namespace: \"" n "\" found.")))))

(defn cljs-namespace?
  "Predicate that returns true if the given string or symbol refers
  to an existing cljs-namespace."
  [s]
  (if (cljs-find-ns s) true false))


(defn ^:private all-ns-vals [] (vals @ana/namespaces))

(defn ^:private all-defs [] (into [] (for [k (all-ns-vals)] (:defs k))))

(defn ^:private all-ns-defs-vals [] (apply concat (for [k (all-defs)] (vals k))))

(defn ^:private all-ns-var-names [] (map :name (all-ns-defs-vals)))

(defn ^:private all-ns-var-names-docs [] (map (fn [e] [(:name e) (:doc e)]) (all-ns-defs-vals)))

(defn ^:private all-ns-clj-cljs-core-publics [] 
  (map (fn [e] (symbol (clojure.string/replace-first (str e) "#'" "")))
       (vals (ns-publics (the-ns 'cljs.core)))))
;;

(defn cljs-apropos
  "Given a regular expression or stringable thing, returns a seq of
all fqnames as symbols in all currently-loaded cljs-namespaces that match the
str-or-pattern. Note that those clj-namespaces with macros are also included."
  [str-or-pattern]
  (let [matches? (if (instance? java.util.regex.Pattern str-or-pattern)
                   #(re-find str-or-pattern (str %))
                   #(.contains (str %) (str str-or-pattern)))]
    (filter matches? (into #{} (concat (all-ns-var-names) (all-ns-clj-cljs-core-publics))))))

(defn cljs-apropos-doc
  "Given a regular expression or stringable thing, return a seq of
all fqnames as symbols in all currently-loaded cljs-namespaces that match the
str-or-pattern in either the fqname or the associated doc-string."
  [str-or-pattern]
  (let [matches? (if (instance? java.util.regex.Pattern str-or-pattern)
                   (fn [e] (or (re-find str-or-pattern (str (first e)))
                                (re-find str-or-pattern (str (second e)))))
                   (fn [e] (or (.contains (str (first e)) (str str-or-pattern))
                               (.contains (str (second e)) (str str-or-pattern)))))]
    (map first (filter matches? (all-ns-var-names-docs)))))


;;;;;


(defn cljs-ns-publics
  "Returns a map of the public intern mappings for the cljs-namespace
  as local-name (symbol) to fq-name (symbol)."
  ([] (cljs-ns-publics ana/*cljs-ns*))
  ([a-ns]
    (when-let [a-ns (and (cljs-namespace? a-ns) (symbol a-ns))]
      (into (sorted-map)
        (map (fn [e] [(key e) (:name (val e))])
          (filter #(not (:private (val %))) (get-in @ana/namespaces
                                                    [a-ns :defs])))))))


(defn cljs-ns-privates
  "Returns a map of the private intern mappings for the cljs-namespace
  as local-name (symbol) to fq-name (symbol)."
  ([] (cljs-ns-privates ana/*cljs-ns*))
  ([a-ns]
    (when-let [a-ns (and (cljs-namespace? a-ns) (symbol a-ns))]
      (into (sorted-map)
        (map (fn [e] [(key e) (:name (val e))])
          (filter #(:private (val %)) (get-in @ana/namespaces
                                                    [a-ns :defs])))))))


(defn cljs-ns-refers-wo-core
  "Returns a map of the refer mappings for the cljs-namespace.
  All the use:...:only, but without the cljs.core contribution."
  ([] (cljs-ns-refers-wo-core ana/*cljs-ns*))
  ([a-ns]
    (when-let [a-ns (and (cljs-namespace? a-ns) (symbol a-ns))]
      (into (sorted-map)
        (map (fn [e] [(key e) (symbol (str (val e) "/" (key e)))])
          (get-in @ana/namespaces [a-ns :uses]))))))


(defn cljs-ns-refers-core
  "Lists all the refered cljs.core bindings for the given cljs-namespace
  minus those cljs.core variables that are :excludes."
  ([] (cljs-ns-refers-core ana/*cljs-ns*))
  ([a-ns]
    (when-let [a-ns (and (cljs-namespace? a-ns) (symbol a-ns))]
      (into (sorted-map)
        (map (fn [e] [(key e) (:name (val e))])
          (filter #(not (:private (val %)))
            (apply dissoc (get-in @ana/namespaces ['cljs.core :defs])
                          (get-in @ana/namespaces [a-ns :excludes]))))))))

(defn cljs-ns-refers
  "Returns a map of the refer mappings for the cljs-namespace.
  All the use:...:only, and cljs.core without the :excludes."
  ([] (cljs-ns-refers ana/*cljs-ns*))
  ([a-ns]
    (when-let [a-ns (and (cljs-namespace? a-ns) (symbol a-ns))]
      (into (sorted-map)
        (merge (cljs-ns-refers-core a-ns) (cljs-ns-refers-wo-core a-ns))))))

(defn cljs-ns-map
  "Returns all the variable lname->fqname mappings for the cljs-namespace.
  (currently misses the macros and the javascript class/function bindings)"
  ([] (cljs-ns-map ana/*cljs-ns*))
  ([a-ns]
    (when-let [a-ns (and (cljs-namespace? a-ns) (symbol a-ns))]
      (into (sorted-map)
        (merge (cljs-ns-refers a-ns)
               (cljs-ns-publics a-ns)
               (cljs-ns-privates a-ns))))))

(defn cljs-ns-interns
  "Returns all the variable lname->fqname mappings for the cljs-namespace.
  (currently misses the macros and the javascript class/function bindings)"
  ([] (cljs-ns-map ana/*cljs-ns*))
  ([a-ns]
    (when-let [a-ns (and (cljs-namespace? a-ns) (symbol a-ns))]
      (into (sorted-map)
        (merge (cljs-ns-privates a-ns)
               (cljs-ns-publics a-ns))))))

(defn cljs-ns-requires
  "Returns the set of required cljs-namespaces for the given cljs-namespace."
  ([] (cljs-ns-requires ana/*cljs-ns*))
  ([a-ns]
    (when-let [a-ns (and (cljs-namespace? a-ns) (symbol a-ns))]
      (into (sorted-set)
        (vals (get-in @ana/namespaces [a-ns :requires]))))))

(defn cljs-ns-aliases
  "Returns a map of the namespace-aliases defined in the cljs-namespace."
  ([] (cljs-ns-aliases ana/*cljs-ns*))
  ([a-ns]
    (when-let [a-ns (and (cljs-namespace? a-ns) (symbol a-ns))]
      (into (sorted-map)
        ;; filter out the trivial aliases-entries where the key equals the val
        (filter (fn [e] (not= (key e) (val e)))
                (get-in @ana/namespaces [a-ns :requires]))))))

(defn cljs-ns-requires-macros
  "Returns a map of the aliases to macro clj-namespaces for this cljs-namespace."
  ([] (cljs-ns-requires-macros ana/*cljs-ns*))
  ([a-ns]
    (when-let [a-ns (and (cljs-namespace? a-ns) (symbol a-ns))]
      (into (sorted-map)
            (get-in @ana/namespaces [a-ns :requires-macros])))))

(defn cljs-ns-macros-clj-ns
  "Returns the set of the clj-namespaces that are required
  for the given cljs-namespace's macros."
  ([] (cljs-ns-macros-clj-ns ana/*cljs-ns*))
  ([a-ns]
    (when-let [a-ns (and (cljs-namespace? a-ns) (symbol a-ns))]
      (into (sorted-set)
        (vals (get-in @ana/namespaces [a-ns :requires-macros]))))))

(defn cljs-all-ns-requires
  "Returns the set of all required FQ-namespaces for all loaded cljs-namespaces.
  If there are ns that are required but not part of loaded,
  then those may be shared and/or macro clj-namespaces."
  []
  (into (sorted-set) (apply clojure.set/union (map cljs-ns-requires (cljs-all-ns)))))
 
(defn cljs-all-ns-requires-macros
  "Returns the set of all required clj-namespaces for a loaded cljs-namespaces."
  []
  (into (sorted-set) (apply clojure.set/union (map cljs-ns-requires-macros (cljs-all-ns)))))
 
(defn cljs-all-missing-ns
  "All the required ns minus the known ns are the missing shared and/or macro clj-ns,
  and the required java-libs.
  We still miss those ns that are required by those clj-ns..."
  [] (into (sorted-set) (clojure.set/difference (cljs-all-ns-requires) (cljs-all-ns))))

(defn cljs-missing-clj-ns
  "The intersection of the set of missing cljs-ns and all the loaded clj-ns
  will give us some of the shared/macro-ns."
  []
  (let [all-ns-str (set (map #(symbol (str %)) (all-ns)))]
    (into (sorted-set) (clojure.set/intersection (cljs-all-missing-ns) all-ns-str))))

(defn cljs-ns-resolve
  "Returns the fqname as a symbol for the var or Class to which
  the given symbol will be resolved in the cljs-namespace, else nil.
  Note that if the symbol is fully qualified,
  the var/Class to which it resolves need not be present in the cljs-namespace."
  ([a-sym] (cljs-ns-resolve ana/*cljs-ns* a-sym))
  ([a-ns a-sym]
    (let [m (binding [ana/*cljs-ns* (symbol a-ns)]
              (ana/resolve-existing-var
                (ana/empty-env)
                (symbol a-sym)))]
      (when (= (:name m) (get-in @ana/namespaces
                           [(:ns m) :defs (symbol (name (:name m))) :name]))
        (:name m)))))

;;;;

(defn cljs-source-fn
  "Returns a string of the cljs-source code for the given symbol, if it can
  find it.  This requires that the symbol resolve to a Var defined in
  a cljs-namespace.  Returns nil if it can't find the source.
  Example: (source-fn 'filter)"
  [x]
  (when-let [v (cljs-ns-resolve x)]
    (let [a-ns (symbol (namespace v))
          a-name (symbol (name v))]
      (when-let [filepath (clojure.string/replace-first
                            (get-in @ana/namespaces
                                  [a-ns :defs a-name :file])
                            "file:"
                            "jar:file:")]
        (when-let [line-no (get-in @ana/namespaces
                                   [a-ns :defs a-name :line])]
          (when-let [strm (clojure.java.io/input-stream filepath)]
            (with-open [rdr (LineNumberReader. (InputStreamReader. strm))]
              (dotimes [_ (dec line-no)] (.readLine rdr))
              (let [text (StringBuilder.)
                    pbr (proxy [PushbackReader] [rdr]
                          (read [] (let [i (proxy-super read)]
                                     (.append text (char i))
                                     i)))]
                (read (PushbackReader. pbr))
                (str text)))))))))


(defmacro cljs-source
  "Prints the cljs-source code for the given symbol, if it can find it.
  This requires that the symbol resolve to a Var defined in a
  cljs-namespace for which the .cljs is in the classpath.

  Example: (source filter)"
  [n]
  `(println (or (cljs-source-fn '~n) (str "Source not found"))))
