(ns chats.models.db
   (:require
     [korma.db :refer [defdb postgres transaction]]
     [korma.core :refer :all]))

(def db (postgres
    {:db   "chats"
     :user "postgres"
     }))
;     :password "Lag1Lag"}))

(defdb korma-db db)

(declare users chats items)

(defentity users
  (has-many chats))

(defentity chats
  (belongs-to users)
  (has-many items))

(defentity items
  (belongs-to chats))

#_(defn create-user [user]
    (insert users (values user)))

#_(defn get-user [id]
    (first (select users
                                    (where {:id id})
                                    (limit 1))))

#_(defn delete-user [id]
    (delete users (where {:id id})))


