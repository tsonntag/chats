(ns chats.models.chat
  (:refer-clojure :exclude [find all])
  (:require
    [korma.core :refer :all]))

(declare chat chat-item)

(defentity chat
    (has-many chat-item))

(defentity chat-item
    (belongs-to chat))

(defn now [] (java.util.Date.))

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

(defn delete! [name]
  (delete chat (where {:name name})))

(defn active? [chat]
  (not (:finished chat)))

(defn active! [chat]
  (update chat 
          (where chat)
          (set-fields {:finished-at (now)})))

(defn pause! [chat]
  (update chat 
          (where chat)
          (set-fields {:finished-at nil})))

(defn toggle-active! [chat]
  (if (active? chat)
    (pause! chat)
    (active! chat)))
