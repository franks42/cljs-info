(ns cljs-info.repl-util
  (:require [cljs.analyzer]
            [cljs.repl]
            [cljs.repl.browser]
            [cljs-info.special-fns-hack]))


(def ^:dynamic *the-repl-env*)
;;   {:port 9000, :optimizations :simple, :working-dir ".lein-cljsbuild-repl", 
;;    :serve-static true, :static-dir ["." "out/"], :preloaded-libs []})


(defn run-repl-listen [port output-dir]
  (let [env (cljs.repl.browser/repl-env 
              :port (Integer. port) 
              :working-dir output-dir 
              :src "src-cljs" 
              :static-dir "resources/public" 
              :serve-static true)]
    (def ^:dynamic *the-repl-env* env)
    (cljs.repl/repl 
      env 
      :special-fns (merge 
                      cljs-info.special-fns-hack/cljs-info-special-fns 
                      {'repl-env (fn [e & p] (print e))
                       'resolve-existing-var 
                         (fn [e v] (print (cljs.analyzer/resolve-existing-var
                                          (cljs.analyzer/empty-env) v)))}))))


