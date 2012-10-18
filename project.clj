(defproject cljs-info "1.0.0"
  :description "Doc and ns-* facilities for ClojureScript"
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [org.clojure/clojurescript "0.0-1503"
                   :exclusions [org.apache.ant/ant]]
                 [clj-info "0.3.1"]
                 [fs42/clj-inspector "0.0.15"]
                 ]
;;   :dev-dependencies [[clj-ns-browser "1.4.0-SNAPSHOT"]]
  :main cljs-info.core)
