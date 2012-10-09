(defproject cljs-info "1.0.0-SNAPSHOT"
  :description "FIXME: write description"
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [org.clojure/clojurescript "0.0-1502"
                   :exclusions [org.apache.ant/ant]]
                 [fs42/clj-inspector "0.0.15"]
                 [clj-info "0.3.0-SNAPSHOT"]
                 ]
  :dev-dependencies [[clj-ns-browser "1.4.0-SNAPSHOT"]]
  :main cljs-info.core)
