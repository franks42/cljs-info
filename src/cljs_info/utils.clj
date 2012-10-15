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


;;;;
;;jsdoc related stuff


(def jsdoc-url-names-mdn-map
  ""
  {
  "https://developer.mozilla.org/en-US/docs/JavaScript/Reference/Global_Objects/"
  #{"Array" "Boolean" "Date" "Function" "Iterator" "Number" "Object" "RegExp" "String" "Error" "EvalError" "InternalError" "RangeError" "ReferenceError" "StopIteration" "SyntaxError" "TypeError" "URIError" "decodeURI" "decodeURIComponent" "encodeURI" "encodeURIComponent" "eval" "isFinite" "isNaN" "parseFloat" "parseInt" "uneval" "Infinity" "JSON" "Math" "NaN" "undefined" }
  
  "https://developer.mozilla.org/en-US/docs/JavaScript_typed_arrays/"
  #{"ArrayBuffer" "DataView" "Float32Array" "Float64Array" "Int16Array" "Int32Array" "Int8Array" "Uint16Array" "Uint32Array" "Uint8Array" "Uint8ClampedArray" }

  "https://developer.mozilla.org/en/JavaScript/Reference/Statements/"
  #{"break" "continue" "debugger" "do...while" "for...in" "function" "if...else" "return" "switch" "throw" "try...catch" "var" "while" "with"}

  "https://developer.mozilla.org/en-US/docs/JavaScript/Reference/Operators/"
  #{"delete" "function" "get" "in" "instanceof" "let" "new" "set" "this" "typeof" "void" "yield"}
  })


(def jsdoc-name-url-mdn-map
  ""
 (sorted-map
   "do" "https://developer.mozilla.org/en/JavaScript/Reference/Statements/do...while"
   "while" "https://developer.mozilla.org/en/JavaScript/Reference/Statements/do...while"
   "for" "https://developer.mozilla.org/en/JavaScript/Reference/Statements/for...in"
   "if" "https://developer.mozilla.org/en/JavaScript/Reference/Statements/if...else"
   "else" "https://developer.mozilla.org/en/JavaScript/Reference/Statements/if...else"
   "try" "https://developer.mozilla.org/en/JavaScript/Reference/Statements/try...catch"
   "catch" "https://developer.mozilla.org/en/JavaScript/Reference/Statements/try...catch"
    "?" "https://developer.mozilla.org/en-US/docs/JavaScript/Reference/Operators/Conditional_Operator"
    "," "https://developer.mozilla.org/en-US/docs/JavaScript/Reference/Operators/Comma_Operator"))


(defn jsdoc-mdn-map
  "Returns a js-name to url map for the specific MDN-webpage."
  []
  (into 
    jsdoc-name-url-mdn-map
    (doall (for [[url names] cljs-info.utils/jsdoc-url-names-mdn-map name names] [name (str url name)]))))


(defn jsdoc-url
  "Returns the best url for the given js-name argument or a search query at MDN."
  [js-name]
  (let [js-name (str js-name)]
    (if-let [specific-url (get (jsdoc-mdn-map) js-name)]
      specific-url
      (str "https://developer.mozilla.org/en-US/search?q=" js-name))))


(defn jsdoc
  "Opens a browser window displaying the javascript-doc for the argument."
  [js-name]
   (if-let [url (jsdoc-url js-name)]
      (clojure.java.browse/browse-url url)
      (println "Could not find JavaScript doc for" js-name)))

