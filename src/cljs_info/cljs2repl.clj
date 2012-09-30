;; Copyright (c) Frank Siebenlist. All rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file COPYING at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns cljs2repl
  (:use ;;[clj-ns-browser.sdoc]
        [cljs-doc]
        [clj-info])
  (:require [cljsbuild.repl.listen]
            [cljs.analyzer]
            [cljs.repl]
            [cljs.repl.browser])
;;             [cljs.repl.server]
;;             [cljs-hacks]
  (:import [clojure.lang LineNumberingPushbackReader]
           [java.io BufferedReader File StringReader]
           [java.lang StringBuilder]))

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
      :special-fns {
;;                     'sdoc (fn [_ quoted-var] 
;;                             (clj-ns-browser.sdoc/sdoc* quoted-var))
                    'doc cljs-doc/cljs-doc-special-fn
                    'cljs-doc cljs-doc/cljs-doc-special-fn
                    'cljs-apropos cljs-doc/cljs-apropos-special-fn
                    '*cljs-ns* (fn [& p] (print cljs.analyzer/*cljs-ns*))
                    'resolve-existing-var (fn [e v] 
                                            (print (cljs.analyzer/resolve-existing-var 
                                                     (cljs.analyzer/empty-env) v)))
                    'cljs-all-ns (fn [& p] (print (cljs-doc/cljs-all-ns)))
                    'cljs-ns-resolve (fn [& p] (print (apply cljs-doc/cljs-ns-resolve (symbify p))))
                    'cljs-ns-map (fn [& p] (print (apply cljs-doc/cljs-ns-map (symbify p))))
                    'cljs-ns-publics (fn [& p] (print (apply cljs-doc/cljs-ns-publics (symbify p))))
                    'cljs-ns-aliases (fn [& p] (print (apply cljs-doc/cljs-ns-aliases (symbify p))))
                    'cljs-ns-requires (fn [& p] (print (apply cljs-doc/cljs-ns-requires (symbify p))))
                    'cljs-ns-privates (fn [& p] (print (apply cljs-doc/cljs-ns-privates (symbify p))))
                    'cljs-ns-refers (fn [& p] (print (apply cljs-doc/cljs-ns-refers (symbify p))))
                    'cljs-ns-refers-wo-core 
                      (fn [& p] (print (apply cljs-doc/cljs-ns-refers-wo-core (symbify p))))
                    'cljs-find-ns (fn [& p] (print (apply cljs-doc/cljs-find-ns (symbify p))))
                    'repl-env (fn [e & p] (print e))
                    })))


;; (defn cljs*>>> 
;;   ([] (cljs*>>> '(js/alert "Yes Way!")))
;;   ([form] 
;;     (#'cljs.repl/eval-and-print 
;;       *the-repl-env* 
;;       the-env 
;;       form)))
    
(defn cljs*>>> 
  ([] (cljs*>>> '(js/alert "Yes Way!")))
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
    
;; (defn cljs*>>> 
;;   ([] (cljs*>>> '(js/alert "Yes Way!")))
;;   ([form] 
;;      (cljs.repl/evaluate-form 
;;         cljsbuild.repl.listen/*the-repl-env* 
;;         (assoc cljsbuild.repl.listen/the-env 
;;           :ns (cljs.analyzer/get-namespace cljs.analyzer/*cljs-ns*)) 
;;         "<cljs repl>"
;;         form
;;         (#'cljs.repl/wrap-fn form))))
;;     
;; (defn cljs*>>> 
;;   ([] (cljs*>>> '(js/alert "Yes Way!")))
;;   ([form] 
;;     (with-bindings {*out* *out*}
;;       (cljs.repl/evaluate-form 
;;         *the-repl-env* 
;;         (assoc the-env :ns (cljs.analyzer/get-namespace cljs.analyzer/*cljs-ns*)) 
;;         "<cljs repl>"
;;         form
;;         (#'cljs.repl/wrap-fn form)))))
    
(defmacro cljs>>> 
  ([] (cljs*>>> '(js/alert "Yes Way!")))
  ([form] (cljs*>>> form))
  ;;([form & forms] (cljs*>>> (concat ['do form] forms)))
;;   ([form & forms] (map cljs*>>> (cons form forms)))
  ([form & forms] (doseq [f (cons form forms)] (cljs*>>> f)))
  )
    
(defn js>>> 
  ([] (js>>> "alert('No Way!')"))
  ([code] 
;;     (println "browser-eval: " (cljs.repl.browser/browser-eval code))))
    (reset! cljs.repl.browser/context-out *out*)
    (let [r (cljs.repl.browser/browser-eval code)]
      (reset! cljs.repl.browser/context-out nil)
      r)))

