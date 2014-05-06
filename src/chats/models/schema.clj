(ns chats.models.schema
  (:require
    [chats.models.db :refer :all]
    [clojure.java.jdbc :as sql]))

(defmacro create-table [& args]
  `(sql/with-connection db (sql/create-table ~@args))) 

(defn create-users-table []
  (create-table :user
                [:id "varchar(32) PRIMARY KEY"]:q
    (sql/with-connection db
          (sql/create-table
                  :users
                  [:id "varchar(32) PRIMARY KEY"]
                  [:pass "varchar(100)"])))

(defn create-images-table []
    (sql/with-connection db
          (sql/create-table
                  :images
                  [:userid "varchar(32)"]
                  [:name "varchar(100)"])))

