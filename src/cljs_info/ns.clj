;; Copyright (c) Frank Siebenlist. All rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file COPYING at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns cljs-info.ns
  (:require [clj-info.doc2txt] ;;clj-info.doc2txt/doc2txt
            [clojure.set]
            [clojure.string]
            [cljs.analyzer]))


(defn cljs-all-ns
  "Returns a sequence of all cljs-namespaces."
  [] (apply sorted-set (keys (namespaces))))

(defn all-ns-vals [] (vals (namespaces)))

(defn all-defs [] (into [] (for [k (all-ns-vals)] (:defs k))))

(defn all-defs-vals [] (apply concat (for [k (all-defs)] (vals k))))

(defn all-var-names [] (map :name (all-defs-vals)))

(defn map-for-ns [a-ns] ((namespaces) a-ns))

(defn defs-for-ns [a-ns] (:defs (map-for-ns a-ns)))

(defn excludes-for-ns [a-ns] (:excludes (map-for-ns a-ns)))

(defn requires-for-ns [a-ns] (:requires (map-for-ns a-ns)))

(defn requires-macros-for-ns [a-ns] (:requires-macros (map-for-ns a-ns)))

(defn name-fqn-for-ns [a-ns] 
  (into (sorted-map) (map (fn [e] [(key e) (:name (val e))]) (defs-for-ns a-ns))))

(defn lnames-for-ns [a-ns] (keys (defs-for-ns a-ns)))

(defn fqnames-for-ns [a-ns] (map :name (vals (defs-for-ns a-ns))))

(defn private-lnames-for-ns [a-ns] (filter #(= \- (first (str %)))(lnames-for-ns a-ns)))

(defn private-fqnames-for-ns [a-ns] (filter #(= \- (first (str (name %))))(fqnames-for-ns a-ns)))

(defn public-lnames-for-ns [a-ns] (filter #(not= \- (first (str %)))(lnames-for-ns a-ns)))

(defn public-fqnames-for-ns [a-ns] (filter #(not= \- (first (str (name %))))(fqnames-for-ns a-ns)))



;;

(defn cljs-apropos
  "Given a regular expression or stringable thing, return a seq of
all definitions in all currently-loaded namespaces that match the
str-or-pattern."
  [str-or-pattern]
  (let [matches? (if (instance? java.util.regex.Pattern str-or-pattern)
                   #(re-find str-or-pattern (str %))
                   #(.contains (str %) (str str-or-pattern)))]
    (filter matches? (all-var-names))))

(defn cljs-apropos-special-fn [e s] (println (cljs-apropos s)))

;;;;;

(defn cljs-ns-publics
  ""
  ([] (cljs-ns-publics (cljs-ns)))
  ([a-ns]
    (when ((cljs-all-ns) a-ns)
      (into (sorted-map) 
        (filter #(not= \- (first (str (key %)))) (name-fqn-for-ns a-ns))))))

(defn cljs-ns-privates
  ""
  ([] (cljs-ns-privates (cljs-ns)))
  ([a-ns]
    (when ((cljs-all-ns) a-ns)
      (into (sorted-map) 
        (filter #(= \- (first (str (key %)))) (name-fqn-for-ns a-ns))))))

(defn cljs-ns-refers-wo-core
  ""
  ([] (cljs-ns-refers-wo-core (cljs-ns)))
  ([a-ns]
    (when ((cljs-all-ns) a-ns)
      (into (sorted-map) 
        (map (fn [e] [(key e) (symbol (str (val e) "/" (key e)))]) (:uses (map-for-ns a-ns)))))))

(defn cljs-ns-refers
  "Returns a map of the refer mappings for the namespace.
  All the use:...:only, and cljs.core without the :excludes."
  ([] (cljs-ns-refers (cljs-ns)))
  ([a-ns]
    (when ((cljs-all-ns) a-ns)
      (let [core-map (cljs-ns-publics 'cljs.core)
            ks (set (keys core-map))
            sub-ks (clojure.set/difference ks (excludes-for-ns a-ns))
            sub-core-map (select-keys core-map sub-ks)]
        (into (sorted-map) (merge (cljs-ns-refers-wo-core a-ns) sub-core-map))))))


(defn cljs-ns-map
  "Returns a map of all the mappings for the namespace.
  (misses javascript bindings)"
  ([] (cljs-ns-map (cljs-ns)))
  ([a-ns]
    (when ((cljs-all-ns) a-ns)
      (into (sorted-map) (merge (cljs-ns-refers a-ns) (cljs-ns-publics a-ns))))))

(defn cljs-ns-aliases
  "Returns a map of the aliases for the namespace."
  ([] (cljs-ns-aliases (cljs-ns)))
  ([a-ns]
    (when ((cljs-all-ns) a-ns)
      (into (sorted-map) (filter (fn [e] (not= (key e) (val e))) (requires-for-ns a-ns))))))

(defn cljs-ns-macro-aliases
  "Returns a map of the aliases to macro clj-namespaces for this namespace."
  ([] (cljs-ns-macro-aliases (cljs-ns)))
  ([a-ns]
    (when ((cljs-all-ns) a-ns)
      (into (sorted-map) (filter (fn [e] (not= (key e) (val e))) (requires-macros-for-ns a-ns))))))

(defn cljs-ns-requires
  "Returns the set of required FQ-namespaces for a namespace."
  ([] (cljs-ns-requires (cljs-ns)))
  ([a-ns]
    (when ((cljs-all-ns) a-ns)
      (into (sorted-set) (vals (requires-for-ns a-ns))))))

(defn cljs-ns-requires-macros
  "Returns the set of required FQ-namespaces for a namespace."
  ([] (cljs-ns-requires-macros (cljs-ns)))
  ([a-ns]
    (when ((cljs-all-ns) a-ns)
      (into (sorted-set) (vals (requires-macros-for-ns a-ns))))))

(defn cljs-all-ns-requires
  "Returns the set of all required FQ-namespaces for a loaded cljs-namespaces.
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

(defn cljs-the-ns 
  "If passed a namespace, returns it. Else, when passed a symbol,
  returns the namespace named by it, throwing an exception if not
  found."
  [n] 
  (or ((cljs-all-ns) (symbol n)) 
      (throw (Exception. (str "No namespace: \"" n "\" found.")))))

(defn cljs-find-ns 
  "Returns the namespace named by the symbol or nil if it doesn't exist."
  [n] 
  ((cljs-all-ns) (symbol n)))

(defn cljs-ns-resolve 
  "Returns the var or Class to which a symbol will be resolved in the
  namespace, else nil.  Note that if the symbol is fully qualified, 
  the var/Class to which it resolves need not be present in the namespace."
  ([a-sym] (cljs-ns-resolve (cljs-ns) a-sym))
  ([a-ns a-sym]
    (let [m (binding [cljs.analyzer/*cljs-ns* (symbol a-ns)]
              (cljs.analyzer/resolve-existing-var 
                (cljs.analyzer/empty-env) 
                (symbol a-sym)))]
      (when (= (:name m) (get-in (namespaces) 
                           [(:ns m) :defs (symbol (name (:name m))) :name]))
        (:name m)))))

