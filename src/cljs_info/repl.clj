;; Copyright (c) Frank Siebenlist. All rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file COPYING at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns cljs-info.repl
  "EXPERIMENTAL - Module provides an alternative run-repl-listen function
  that starts the cljs-repl from within a clj-repl. After that, cljs->repl,
  cljs->repl* and js->repl can be used to send either cljs-forms or js-code
  to the repl and js-vm for eval. Furthermore, some of cljs-info.doc and
  cljs-info.ns functions are made available in the cljs-repl thru the
  special-fns parameter in the cljs.repl/repl function (ugly hack...
  hopefully the reflection facility will come up with a better solution soon)"
  (:use [cljs-info.ns]
        [cljs-info.doc])
  (:require [cljs-info.special-fns-hack]
            [cljs.analyzer]
            [cljs.repl]
            [cljs.repl.browser]
            [cljs.repl.server]))


;; starting of repl with custom init params

(def ^:dynamic *the-repl-env*)
;;   {:port 9000, :optimizations :simple, :working-dir ".lein-cljsbuild-repl",
;;    :serve-static true, :static-dir ["." "out/"], :preloaded-libs []})


(defn run-repl-listen
  "Redefinition of lein-cljsbuild's run-repl-listen, such that we can set our own init-params for the repl-env, which we need for cljs->repl context.
  Repl is started with: (run-repl-listen)"
  ([] (run-repl-listen 9000 ".lein-cljsbuild-repl"))
  ([port output-dir]
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
                                          (cljs.analyzer/empty-env) v)))})))))


;; redefine the :print handle-post handler such that it optionally prints "locally".

(def context-out (atom true))

(defn set-local-printing!
  "Boolean input determines whether the stdout for explicit printing
  to *out* inside the evaluated cljs-forms or js-code is redirected to
  the local *out* of the calling context (true), or send to the
  repl-terminal's stdout (false)."
  [true-false]
  (reset! cljs-info.repl/context-out true-false))

;; monkey patching the "cljs.repl.browser/handle-post :print" code
;; no elegant solution until clojurescript provides the knob...
(defmethod cljs.repl.browser/handle-post :print [{:keys [content order]} conn _ ]
  (if @cljs-info.repl/context-out
    (do (cljs.repl.browser/constrain-order
          order (fn [] (binding [*out* @cljs-info.repl/context-out]
                                        (do (print (read-string content))
                                            (.flush *out*)))))
        (cljs.repl.server/send-and-close conn 200 "ignore__"))

    (do (cljs.repl.browser/constrain-order
          order (fn [] (do (print (read-string content)) (.flush *out*))))
        (cljs.repl.server/send-and-close conn 200 "ignore__"))))

;;;;;;;;


(def the-env
  "required init env value for cljs.repl/evaluate-form"
  {:context :statement :locals {}})


(defn cljs->repl*
  "Functions compiles the passed clojurescript form, sends the resulting javascript
  to the browser where it's evaluated, sends the result back to the clj-side, and
  returns that eval-result.
  If so configured, any printing to *out* from within the cljs-form is redirected to
  the stdout connected to the calling context."
  ([] (cljs->repl* '(js/alert "Yes Way!")))
  ([form]
    ;; see if we have to redirect stdout to our local *out*
    (when-not (false? @cljs-info.repl/context-out)
      (reset! cljs-info.repl/context-out *out*))
    ;; send the form for eval
    (let [eval-result (cljs.repl/evaluate-form
              *the-repl-env*
              (assoc the-env
                :ns (cljs.analyzer/get-namespace cljs.analyzer/*cljs-ns*))
              "<cljs repl>"
              form
              (#'cljs.repl/wrap-fn form))]
      ;; reset stdout if needed
      (when-not (false? @cljs-info.repl/context-out)
        (reset! cljs-info.repl/context-out nil))
      eval-result))
  ([form & forms] (doseq [f (cons form forms)] (cljs->repl* f))))
;;   ([form & forms] (cljs->repl* (into [] (cons form forms)))))

(defmacro cljs->repl
  "Macro compiles the clojurescript form(s),
  sends the resulting javascript to the browser,
  evaluates the js, sends the result back to the clj-side, and returns that result.
  Any printing to *out* from within the cljs-form is send to the stdout connected to the calling context."
  ([] (cljs->repl* '(js/alert "Yes Way!")))
  ([form] (cljs->repl* form))
  ([form & forms] (doseq [f (cons form forms)] (cljs->repl* f))))
;;   ([form & forms] (cljs->repl* (into [] (cons form forms)))))


(defn js->repl
  "Function sends the javascript code (string) to the browser,
  evaluates the js, sends the result back to the clj-side, and returns that result.
  Any printing to *out* from within the js-code is send to the stdout connected to the calling context."
  ([] (js->repl "alert('No Way!')"))
  ([code]
    (when-not (false? @cljs-info.repl/context-out)
      (reset! cljs-info.repl/context-out *out*))
    (let [r (cljs.repl.browser/browser-eval code)]
      (when-not (false? cljs-info.repl/context-out)
        (reset! @cljs-info.repl/context-out nil))
      r)))

