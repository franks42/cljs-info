(ns cljs-info.patch-cljs-repl-browser)

(def qwe 123)

(in-ns 'cljs.repl.browser)
  
(def context-out (atom nil))

(defmethod handle-post :print [{:keys [content order]} conn _ ]
  (if @context-out
    (do (constrain-order order (fn [] (binding [*out* @context-out] 
                                        (do (print (read-string content))
                                            (.flush *out*)))))
        (server/send-and-close conn 200 "ignore__"))

    (do (constrain-order order (fn [] (do (print (read-string content))
                                         (.flush *out*))))
        (server/send-and-close conn 200 "ignore__"))))
