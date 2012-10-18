(ns cljs-info.special-fns-hack
  "TEMPORARY HACK - Module to generate the special-fns maps for use with repl/repl."
  (:require [cljs-info.ns]
            [clojure.pprint]
            [cljs-info.doc]))

(declare cljs-info-special-fns)

(defn symbify
  "Hack to provide some robustness on the repl-input
  (the repl crashes too easily when unexpected characters or var-types are entered)"
  [p]
  (map
    (fn [n] (symbol (str (if (= (type n) clojure.lang.Cons) (second n) n))))
    (rest p)))


;; (def cljs-ns-browser-special-fns
;;   "Function mapping table for use with run-repl-listen."
;;   {'sdoc (fn [_ & quoted-var]
;;           (if (seq quoted-var)
;;             (clj-ns-browser.sdoc/sdoc* (first quoted-var))
;;             (clj-ns-browser.sdoc/sdoc*)))})


(def cljs-doc-special-fns
  "Function mapping table for use with run-repl-listen."
  {'cljs-doc (fn [& p] (print (apply cljs-info.doc/cljs-doc* (symbify p))))
   'cljs-find-doc (fn [& p] (print (apply cljs-info.doc/cljs-find-doc (symbify p))))
   })


(def cljs-ns-special-fns
  "Function mapping table for use with run-repl-listen."
  {
  'cljs-info (fn [& p] (println "Available \"special\" functions:")
                       (clojure.pprint/pprint (sort (keys cljs-info-special-fns))))
  'cljs-ns (fn [& p] (print (cljs-info.ns/cljs-ns)))
  'cljs-source (fn [& p] (print (cljs-info.ns/cljs-source-fn (second p))))
  'cljs-apropos (fn [& p] (print (cljs-info.ns/cljs-apropos (second p))))
  'cljs-apropos-doc (fn [& p] (print (cljs-info.ns/cljs-apropos-doc (second p))))
  '*cljs-ns* (fn [& p] (print cljs.analyzer/*cljs-ns*))
  'cljs-all-ns (fn [& p] (print (cljs-info.ns/cljs-all-ns)))
  'cljs-ns-resolve (fn [& p] (print (apply cljs-info.ns/cljs-ns-resolve (symbify p))))
  'cljs-ns-map (fn [& p] (print (apply cljs-info.ns/cljs-ns-map (symbify p))))
  'cljs-ns-publics (fn [& p] (print (apply cljs-info.ns/cljs-ns-publics (symbify p))))
  'cljs-ns-aliases (fn [& p] (print (apply cljs-info.ns/cljs-ns-aliases (symbify p))))
  'cljs-ns-requires (fn [& p] (print (apply cljs-info.ns/cljs-ns-requires (symbify p))))
  'cljs-ns-privates (fn [& p] (print (apply cljs-info.ns/cljs-ns-privates (symbify p))))
  'cljs-ns-refers (fn [& p] (print (apply cljs-info.ns/cljs-ns-refers (symbify p))))
  'cljs-ns-refers-wo-core
    (fn [& p] (print (apply cljs-info.ns/cljs-ns-refers-wo-core (symbify p))))
  'cljs-find-ns (fn [& p] (print (apply cljs-info.ns/cljs-find-ns (symbify p))))
  })


(def cljs-info-special-fns
  "Function mapping table for use with run-repl-listen."
;;   (merge cljs-doc-special-fns cljs-ns-special-fns cljs-ns-browser-special-fns))
  (merge cljs-doc-special-fns cljs-ns-special-fns))
