(ns chats.routes.home
  (:require
    [compojure.core :refer :all]
    [chats.views.layout :as layout]
    [chats.views.chat :as view]
    ))

(defroutes home-routes
  (GET "/" []
       (layout/common
         [:h1 "Welcome to chats!"]))

  (GET "/chats" []
       (layout/common
         (view/all)))

  (GET "/chats/new" [] 
       (layout/common
         (view/new)))

  (GET "/chats/:id" [id]
       (layout/common
         (view/show (Integer/parseInt id))))
  )
