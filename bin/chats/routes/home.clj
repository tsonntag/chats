(ns chats.routes.home
  (:require
    [compojure.core :refer :all]
    [chats.views.layout :as layout]
    [chats.models.schema :as schema]
    ))

(defn home []
  (layout/common [:h1 "Hello World!"]))

(defn chats []
  (layout/common [:h1 "Chats"]
                 [:ul 
                  (for [chat (schema/chats)]
                    [:li (:name chat)])]
                 ))

(defroutes home-routes
  (GET "/"      [] (home))
  (GET "/chats" [] (chats)))
