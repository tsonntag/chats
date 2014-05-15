(ns chats.views.chat
  (:require 
    [clojure.string :refer [capitalize]]
    [chats.models.chat :as chat]
    [hiccup.element :refer :all]))

(defn prop-val [obj arg]
  (println "AAAAAAAAAAprop-val" obj arg)
  (cond
    (ifn? arg) (arg obj)
    :else arg))

(defn prop-key [arg]
   (cond
     (keyword? arg) (capitalize (name arg))
     (vector? arg)  (first arg)
     :else arg))

(defn table* [heads rows]
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

(defn table [objs cols]
  (let [heads (map prop-key cols)
        rows  (map (fn [obj] 
                     (map #(prop-val obj %) cols))
                   objs)]
    (table* heads rows)))
    

(defn prop-table [obj & args]
  (for [arg args]
    [:div.row
     [:strong.col-md-2 (str (prop-key arg) ":")]
     [:div.col-md-4    (prop-val obj arg)          ]]))

(defn link [chat]
  (link-to (format "/chats/%s" (:id chat)) (:name chat)))

(defn all
  ([chats]
    (list
      [:h1 "Chats"]
      (table ["Name" "Created" "Active" "Items"]
             (for [chat chats]
               [(link chat) (:created-at chat) (chat/active? chat) (count (:chat-item chat))]))))
  ([]
   (all (chat/all))))

(defn show [id]
  (if-let [chat (chat/find id)]
    (list
      [:h1 "Chat"]
      (prop-table chat
                  :name
                  :created-at
                  ["Active" (chat/active? chat)])
      (table
        ["Id" "Created" "Request" "Response" "Response forwarded"]
        []))

    [:h1 "Not found"]))


(defn new [])
