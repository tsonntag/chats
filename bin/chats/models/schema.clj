(ns chats.models.schema
  (:require
    [clojure.string :as str :only [replace-first]]
    [cemerick.url :refer [url]]
    [korma.db :refer [defdb default-connection]]
    [korma.core :refer :all]))

(def db-url (or (System/getenv "DATABASE_URL") "postgres://postgres:@localhost:5432/chats"))

(def db-spec
  (let [url (url (str/replace-first db-url #"[^:]*:" "http:"))]
   {:classname "org.postgresql.Driver"
    :subprotocol "postgresql"
    :user (:username url)
    :password (:password url)
    :subname (str "//" (:host url) ":" (:port url) (:path url))}))

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

