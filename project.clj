(defproject cljs-info "1.0.0-SNAPSHOT"
  :description "Doc and ns-* facilities for ClojureScript"
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [org.clojure/clojurescript "0.0-1508"
                   :exclusions [org.apache.ant/ant]]
                 [fs42/clj-inspector "0.0.15"]
                 [clj-info "0.3.0"]
                 ]
  :dev-dependencies [[clj-ns-browser "1.4.0-SNAPSHOT"]]
  :main cljs-info.core)
