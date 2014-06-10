(ns chats.routes.gui
  (:require
    [compojure.core :refer :all]
    [chats.views.layout :as layout]
    [chats.models.chat :refer :all]
    [chats.views.utils :refer :all]
    [noir.response :refer [redirect]]
    [taoensso.timbre :refer [info debug]]
    [hiccup.element :refer :all]
    [hiccup.form :refer :all]))

(defn- link [chat]
  (link-to (format "/chats/%s" (:id chat)) (:name chat)))

(defn- link-toggle [_ chat]
  (let [icon (if (chat-active? chat)
               (icon-stop :title "Deactivate")
               (icon-play :title "Activate"))]
    (put-link (format "/chats/%s/toggle" (:id chat)) icon)))

(defn- link-clear [_ id]
   (put-link (format "/chats/%s/clear" id) (icon-clear :title "Delete items")))

(defn- redirect-to [id]
  (redirect (str "/chats/" id)))

(defn all
  ([chats]
    (layout/common
      [:h1 "Chats"]
      (links "chats" link-new)
      (table chats
             ["Name" link]
             :created-at
             ["Active" chat-active?]
             ["Items"  item-count])))
  ([]
   (all (chats))))

(defn show [id]
  (if-let [chat (find-chat :id id)]
    (layout/common
      [:h1 "Chat"]
      (links "chats" link-list (link-delete id) (link-clear id) (link-toggle chat))
      (prop-table chat
                  :name
                  :created-at
                  ["Active" (chat-active? chat)])
      [:br]
      (table (sort-by :created-at (:chat-item chat))
             :id
             :created-at
             :request
             :response
             ["Response forwarded" :responded-at]))
    {:status 404
     :body (format "chat=%s\nmsg=not found\n" id)}))

(defn new-chat []
   (layout/common
     [:h1 "Create Chat"]
     [:br]
     (form-to [:post "/chats"]
              (control text-field :name "Name:")
              [:br]
              (submit-button "Create"))))

(defn create [name]
  (chat-create! name)
  (redirect "/chats"))

(defn delete [id]
  (chat-delete! id)
  (redirect "/chats"))

(defn toggle [id]
  (chat-toggle-active! id)
  (redirect-to id))

(defn clear [id]
  (chat-clear! id)
  (redirect-to id))


(defroutes gui-routes
    (GET    "/chats"            []     (all))
    (POST   "/chats"            [name] (create name))
    (GET    "/chats/new"        []     (new-chat))
    (GET    "/chats/:id"        [id]   (show   (Integer/parseInt id)))
    (PUT    "/chats/:id/toggle" [id]   (toggle (Integer/parseInt id)))
    (PUT    "/chats/:id/clear"  [id]   (clear  (Integer/parseInt id)))
    (DELETE "/chats/:id"        [id]   (delete (Integer/parseInt id)))
  )

