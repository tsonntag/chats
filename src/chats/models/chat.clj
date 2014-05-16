(ns chats.models.chat
  (:refer-clojure :exclude [find all])
  (:require
    [korma.core :refer :all]))

(declare chat chat-item)

(defentity chat
    (has-many chat-item))

(defentity chat-item
    (belongs-to chat))

(defn now [] (java.sql.Timestamp (System/currentTimeMillis)))

(defn add! [name]
  (insert chat (values [{:name name}])))

(defn add-item! [item]
  (insert chat-item (values [item])))

(defn find [id]
  (first (select chat (where {:id id}))))

(def chats* 
  (-> (select* chat)
      (with chat-item)
      (order :name :ASC)))

(defn all []
  (-> chats*
      (select)))

(defn active []
  (-> chats*
      (where {:finished-at nil})
      (select)))

(defn delete! [id]
  (delete chat (where {:id id})))

(defn active? [chat]
  (not (:finished-at chat)))

(defn active! [id]
  (update chat 
          (where {:id id})
          (set-fields {:finished-at nil})))

(defn pause! [id]
  (update chat 
          (where {:id id})
          (set-fields {:finished-at (now)})))

(defn toggle-active! [id]
  (when-let [chat (find id)]
    (println "CCCCCCCCCCC" chat)
    (if (active? chat)
      (pause! id)
      (active! id))))

(defn clear! [id]
  (delete chat-item (where {:chat_id id})))
