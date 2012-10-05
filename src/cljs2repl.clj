;; Copyright (c) Frank Siebenlist. All rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file COPYING at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns cljs2repl
  (:use ;;[clj-ns-browser.sdoc]
        [cljs-ns]
        [cljs-doc]
        [clj-info])
  (:require [cljsbuild.repl.listen]
            [cljs.analyzer]
            [cljs.repl]
            [cljs.repl.browser])
  (:import [clojure.lang LineNumberingPushbackReader]
           [java.io BufferedReader File StringReader]
           [java.lang StringBuilder]))

    
(def ^:dynamic *the-repl-env*)
;;   {:port 9000, :optimizations :simple, :working-dir ".lein-cljsbuild-repl", 
;;    :serve-static true, :static-dir ["." "out/"], :preloaded-libs []})

(def the-env 
  {:context :statement :locals {}})
  

(defn cljs*->repl
  "Functions compiles the clojurescript form, sends the resulting javascript to the browser,
  evaluates the js, sends the result back to the clj-side, and returns that result.
  Any printing to *out* from within the cljs-form is send to the stdout connected to the calling context."
  ([] (cljs*->repl '(js/alert "Yes Way!")))
  ([form]
     (reset! cljs.repl.browser/context-out *out*)
     (let [r (cljs.repl/evaluate-form 
                *the-repl-env* 
                (assoc the-env 
                  :ns (cljs.analyzer/get-namespace cljs.analyzer/*cljs-ns*)) 
                "<cljs repl>"
                form
                (#'cljs.repl/wrap-fn form))]
       (reset! cljs.repl.browser/context-out nil)
       r)))
    
    
(defmacro cljs->repl 
  "Macro compiles the clojurescript form(s), 
  sends the resulting javascript to the browser,
  evaluates the js, sends the result back to the clj-side, and returns that result.
  Any printing to *out* from within the cljs-form is send to the stdout connected to the calling context."
  ([] (cljs*->repl '(js/alert "Yes Way!")))
  ([form] (cljs*->repl form))
  ([form & forms] (doseq [f (cons form forms)] (cljs*->repl f)))
  )
    
(defn js->repl
  "Function sends the javascript code (string) to the browser,
  evaluates the js, sends the result back to the clj-side, and returns that result.
  Any printing to *out* from within the js-code is send to the stdout connected to the calling context."
  ([] (js->repl "alert('No Way!')"))
  ([code] 
    (reset! cljs.repl.browser/context-out *out*)
    (let [r (cljs.repl.browser/browser-eval code)]
      (reset! cljs.repl.browser/context-out nil)
      r)))

