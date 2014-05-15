(ns chats.views.layout
  (:require 
    [hiccup.page :refer [html5 include-css include-js]]
    [hiccup.element :refer :all]))

(defn menu []
  [:div.navbar.navbar-inverse.navbar-fixed-top {:role "navigation"}
   [:div.container
    [:div.navbar-header
     (link-to {:class "navbar-brand"} "/chats" "Chats")]
    [:div
     [:ul.nav.navbar-nav
      ;[:li (link-to "/chats" "Chats")]
      ]
     [:ul.nav.navbar-nav.pull-right
      [:li.divider-vertical]
      [:li (link-to "/about" "About")]]]]])

(defn common [& body]
  (println body)
  (html5
    [:head
     [:title "Chats"]
     (include-css "/css/bootstrap.min.css")
     (include-css "/css/chats.css")
    [:body 
     (menu)
     [:div.container
      [:div.row
       [:div.col-md-12 body]]]
     (include-js "https://ajax.googleapis.com/ajax/libs/jquery/1.11.0/jquery.min.js")
     (include-js "/js/bootstrap.min.js")]]))
