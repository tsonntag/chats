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

(defn create! [name]
  (insert chat (values [{:name name}])))

(defn add-item! [item]
  (insert chat-item (values [item])))

(defmacro find [& arg]
  `(first (select chat 
                  (with chat-item)
                  (where (assoc {} ~@arg)))))

(defmacro find-item [& arg]
  ` (first (select chat-item
                   (where (assoc {} ~@arg)))))

(defn not-responded-items [chat]
  (filter (comp not :response) (:chat-item chat))) 

(defn response-not-forwarded-items [chat]
  (filter (comp not :responded-at) (:chat-item chat))) 

(defn all []
  (select chat
          (with chat-item)
          (order :name :ASC)))

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

(defn item-responded! [item]
  (update chat-item 
          (where {:id (:id item)})
          (set-fields {:responded-at (now)})))

(defn item-response! [id rsp]
  (update chat-item 
          (where {:id id})
          (set-fields {:response rsp})))

(defn item-obsolete! [item]
  (update chat-item 
          (where {:id (:id item)})
          (set-fields {:response (str (:response item) "<OBSOLETE>") :responded-at (now)})))

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
