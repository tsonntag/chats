(ns chats.models.chat
  (:refer-clojure :exclude [find all])
  (:require
    [korma.core :refer :all]))

(declare chat chat-item)

(defentity chat
    (has-many chat-item))

(defentity chat-item
    (belongs-to chat))

(defn now [] (java.sql.Timestamp. (System/currentTimeMillis)))

(defn add! [name]
  (insert chat (values [{:name name}])))

(defn add-item! [item]
  (insert chat-item (values [item])))

(defn find [& arg]
  (first (select chat (where (apply assoc {} arg)))))

(defn find-item [& arg]
  (first (select chat-item (where (apply assoc {} arg)))))

(defn find-active []
  (first (select chat (where {:finished-at nil}))))

(defn not-responded-items [chat]
  (filter (comp not :response) (:chat-item chat))) 

(defn responded-items [chat]
  (filter :response (:chat-item chat))) 

(defn response-not-forwarded-items [chat]
  (filter (comp not :responded-at) (:chat-item chat))) 

(def chats* 
  (-> (select* chat)
      (with chat-item)
      (order :name :ASC)))

(defn all []
  (-> chats*
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


(defn item-count [chat]
  (-> (select chat-item
          (where {:chat_id (:id chat)})
          (aggregate (count :*) :count))
      first
      :count))
