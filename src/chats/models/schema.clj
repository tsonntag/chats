(ns chats.models.schema
  (:require
    [korma.db :refer [defdb]]
    [korma.core :refer :all]))

(def db-spec
  (or (System/getenv "DATABASE_URL")
  {:classname "org.postgresql.Driver"
   :subprotocol "postgresql"
   :user "postgres"
   :subname "//localhost:5432/chats"}))

(defdb korma-db db-spec)

(declare chats chat-items)

(defentity chats
  (has-many chat-items))

(defentity chat-items
  (belongs-to chats))


(defn add-chat [name]
  (insert :chats (values [{:name name}])))

(defn chats []
  (select :chats))

(defn delete-chat [name]
  (delete :chats (where {:name name})))

