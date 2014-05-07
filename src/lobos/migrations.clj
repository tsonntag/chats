(ns lobos.migrations
  (:refer-clojure :exclude [alter drop bigint boolean char double float time])
  (:use (lobos [migration :only [defmigration]] core schema
               config helpers)))

(println "lobos.migrations..")
(defmigration create-chats
  (up []
      (create
        (table :chats
               (surrogate-key)
               (varchar :name 128)
               (integer :active-item-id)
               (timestamp :finished-at)
               (timestamps)))
      (create
        (index :chats [:name])) 

      (create
        (table :chat-items
               (surrogate-key)
               (integer :chat-id)
               (text    :request)
               (text    :response)
               (timestamp :responded-at)
               (timestamps)))
      (create
        (index :chat-items [:chat-id])))
  (down []
        (drop (table :chats))
        (drop (table :chat-items))))
