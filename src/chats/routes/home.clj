(ns chats.routes.home
  (:require
    [compojure.core :refer :all]
    [chats.views.layout :as layout]
    [chats.models.chat :as chat] 
    ))

(defn home []
  (layout/common [:h1 "Hello World!"]))

(defn chats []
  (layout/common [:h1 "Chats"]
                 (map #(vector [:h2 (:name %)]) (chat/all))
                 ))

(defroutes home-routes
  (GET "/"      [] (home))
  (GET "/chats" [] (chats)))
