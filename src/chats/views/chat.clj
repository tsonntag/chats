(ns chats.views.chat
  (:require 
    [clojure.string :refer [capitalize]]
    [chats.models.chat :as chat]
    [chats.views.layout :as layout]
    [chats.views.utils :refer :all]
    [noir.response :refer [redirect]]
    [hiccup.element :refer :all]
    [hiccup.form :refer :all]))

(defn link [chat]
  (link-to (format "/chats/%s" (:id chat)) (:name chat)))

(defn all
  ([chats]
    (layout/common
      [:h1 "Chats"]
      (links "chats" link-new)
      (table chats
             ["Name" link]
             :created-at
             ["Active" chat/active?]
             ["Items" (comp count :chat-item)])))
  ([]
   (all (chat/all))))

(defn show [id]
  (if-let [chat (chat/find id)]
    (layout/common
      [:h1 "Chat"]
      (links "chats" link-list (link-delete id))
      (prop-table chat
                  :name
                  :created-at
                  ["Active" (chat/active? chat)])
      [:br]
      (table (:chat-item chat)
             :id
             :created-at
             :request
             :response
             ["Response forwarded" :responded-at]))
    [:h1 "Not found"]))


(defn new []
   (layout/common
     [:h1 "Create Chat"]
     [:br]
     (form-to [:post "/chats"]
              (control text-field :name "Name:")
              [:br]
              (submit-button "Create"))))

(defn create [name]
  (chat/add! name)
  (redirect "/chats"))

(defn delete [id]
  (chat/delete! id)
  (redirect "/chats"))
