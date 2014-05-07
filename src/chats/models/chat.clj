(ns chats.models.chat
  (:require
    [korma.core :refer :all]))

(defn add [name]
  (insert :chats (values [{:name name}])))

(defn all []
  (select :chats))

(defn remove [name]
  (delete :chats (where {:name name})))


