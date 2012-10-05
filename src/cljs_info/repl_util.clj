(ns cljs-repl-util)


(def ^:dynamic *the-repl-env*)
;;   {:port 9000, :optimizations :simple, :working-dir ".lein-cljsbuild-repl", 
;;    :serve-static true, :static-dir ["." "out/"], :preloaded-libs []})

(def the-env 
  {:context :statement :locals {}})
  
(defn symbify [p] (map 
                    (fn [n] (symbol (str (if (= (type n) clojure.lang.Cons) (second n) n)))) 
                    (rest p)))


(defn run-repl-listen [port output-dir]
  (let [env (cljs.repl.browser/repl-env :port (Integer. port) :working-dir output-dir :src "src-cljs")]
    (def ^:dynamic *the-repl-env* env)
    (cljs.repl/repl 
      env 
      :special-fns (merge 
                      cljs-ns/cljs-ns-special-fns 
                      cljs-doc/cljs-doc-special-fns 
                      {
                        'sdoc (fn [_ & quoted-var] 
                                (if (seq quoted-var)
                                  (clj-ns-browser.sdoc/sdoc* (first quoted-var))
                                  (clj-ns-browser.sdoc/sdoc*)))

                        'repl-env (fn [e & p] (print e))
                        'resolve-existing-var 
                          (fn [e v] (print (cljs.analyzer/resolve-existing-var
                                           (cljs.analyzer/empty-env) v)))}))))


