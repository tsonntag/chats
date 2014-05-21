(ns lobos.migrations
  (:refer-clojure :exclude [alter drop bigint boolean char double float time])
  (:use (lobos [migration :only [defmigration migrations]] core schema config helpers)))

(defmigration create-chats
  (up []
      (create
        (table :chat
               (surrogate-key)
               (varchar :name 128)
               (integer :active-item_id)
               (timestamp :finished-at)
               (timestamps)))
      (create
        (index :chat [:name])) 

      (create
        (table :chat-item
               (surrogate-key)
               (integer :chat_id)
               (text    :request)
               (text    :response)
               (timestamp :responded-at)
               (timestamps)))
      (create
        (index :chat-item [:chat_id])))

  (down []
        (drop (table :chat))
        (drop (table :chat-item))))
