(ns chats.views.layout
  (:require 
    [hiccup.page :refer [html5]]
    [hiccup.bootstrap.page :refer [include-bootstrap]]))

(defn common [& body]
  (html5
    [:head
     [:title "Welcome to chats"]
     (include-bootstrap)]
    [:body body]))
