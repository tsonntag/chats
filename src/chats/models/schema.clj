(ns chats.models.schema
  (:require
    [clojure.string :refer [replace-first]]
    [cemerick.url :refer [url]]
    [korma.db :refer [defdb default-connection]]))

(def db-url (or (System/getenv "DATABASE_URL") "postgres://postgres:@localhost:5432/chats"))

(def db-spec
  (let [url (url (replace-first db-url #"[^:]*:" "http:"))]
   {:classname "org.postgresql.Driver"
    :subprotocol "postgresql"
    :user (:username url)
    :password (:password url)
    :subname (str "//" (:host url) ":" (:port url) (:path url))}))

(defdb korma-db db-spec)
