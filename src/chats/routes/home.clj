(ns chats.routes.home
  (:require
    [compojure.core :refer :all]
    [chats.views.layout :as layout]
    ))

(defroutes home-routes
  (GET "/" []
       (layout/common
         [:h1 "Welcome to chats!"])))
