(ns chats.views.chat
  (:require 
    [chats.models.chat :as chat]
    [hiccup.element :refer :all]))

(defn table [heads rows]
  [:table.table.table-striped
   [:thead
    [:tr
     (for [head heads]
       [:th head])]]
   [:tbody
    (for [row rows]
      [:tr
       (for [el row] 
         [:td el])])]])

(defn link [chat]
  (let [name (:name chat)]
    (link-to (format "/chats/%s" name) name)))

(defn all [chats]
  (table ["Name" "Created" "Active" "Items"]
         (for [chat chats]
           [(link chat) (:created-at chat) (chat/active? chat) (count (:chat-items chat))])))
