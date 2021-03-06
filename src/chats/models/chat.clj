(ns chats.models.chat
  (:require
    [korma.core :refer :all]))

(declare chat chat-item)

(defentity chat
    (has-many chat-item))

(defentity chat-item
    (belongs-to chat))

(defn now [] (java.sql.Timestamp. (System/currentTimeMillis)))

(defn chat-create! [name]
  (insert chat (values [{:name name}])))

(defn add-item! [item]
  (insert chat-item (values [item])))

(defmacro find-chat [& arg]
  `(first (select chat
                  (with chat-item)
                  (where (assoc {} ~@arg)))))

(defmacro items [& forms]
  `(select chat-item ~@forms))

(defn for-chat [query chat]
  (where query {:chat_id (:id chat)}))

(defmacro find-item [item & forms]
  `(first
    (items (where (select-keys ~item [:id]))
           ~@forms)))

(defn not-responded [query]
  (where query {:response nil}))

(defn responded [query]
  (where query {:response [not= nil]}))

(defn response-not-forwarded [query]
  (where query {:responded-at nil}))

(defn chats []
  (select chat
          (with chat-item)
          (order :name :ASC)))

(defn chat-delete! [id]
  (delete chat (where {:id id})))

(defn chat-active? [chat]
  (not (:finished-at chat)))

(defn chat-active! [id]
  (update chat
          (where {:id id})
          (set-fields {:finished-at nil})))

(defn chat-pause! [id]
  (update chat
          (where {:id id})
          (set-fields {:finished-at (now)})))

(defn item-responded! [id]
  (update chat-item
          (where {:id id})
          (set-fields {:responded-at (now)})))

(defn item-response! [id rsp]
  (update chat-item
          (where {:id id})
          (set-fields {:response rsp})))

(defn item-obsolete! [item]
  (update chat-item
          (where {:id (:id item)})
          (set-fields {:response (str (:response item) "(OBSOLETE)") :responded-at (now)})))

(defn chat-toggle-active! [id]
  (when-let [chat (find-chat id)]
    (if (chat-active? chat)
      (chat-pause! id)
      (chat-active! id))))

(defn chat-clear! [id]
  (delete chat-item (where {:chat_id id})))


(defn item-count [chat]
  (-> (select chat-item
          (where {:chat_id (:id chat)})
          (aggregate (count :*) :count))
      first
      :count))
