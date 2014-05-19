(ns chats.routes.api
  (:refer-clojure :exclude [list])
  (:require
    [clojure.string :as str :only [join]]
    [chats.models.chat :as chat]
    [noir.response :refer [redirect]]))

(defn redirect-to [id]
  (redirect (str "/chats/" id)))

(defn render [& {:keys [status] :or {status 200} :as arg}]
  (let [hash (dissoc arg :status)
        body (str/join "\n" (for [[k v] hash] (format "%s=%s" (name k) v)))]
    {:status status :body body}))

(defmacro with-chat [chat id & body]
  `(if-let [~chat (chat/find ~id)]
     (do ~@body)
     (render :status 404 :chat ~id :msg "Not found")))

(defn list []
  (let [lines (map #(format "%-10s: %4d\n" (:name %) (chat/item-count %)) (chat/all))
        head  "#chat items\n"
        body  (apply str (cons head lines))]
    {:status 200 :body  body}))

(defn query [id]
  (with-chat chat id
     (let [fmt    "%s;%s;%s;%s;%s\n"
           head   ["#item" "created" "request" "response" "response_forwarded"] 
           rows   (map #((juxt :id :created-at :request :response :responded-at) %) (:chat-item chat))
           lines  (map #(apply format fmt %) (cons head rows))
           title  (format "chat: %s\n" (:name chat))
           body   (apply str (cons title lines))]
       {:status 200 :body body})))

(defn delete [id]
  (with-chat chat id
    (println "CCCCCC" chat id )
    (chat/delete! id)
    (render :chat (:name chat))))

(defn do-create [id force]
  (if-let [chat (chat/find id)]
    (if force
      (do
        (chat/clear!)
        (render :msg "created"))
      (render :status 400
              :chat id
              :msg "already exists"))))
    ;(if-let [chat (chat/create! id)


(defn create [id])

(defn init [id])

(defn get-rsp [])

(defn post-rsp [])

(defn get-req [])

(defn post-req [])

#_(defn -create [name]
  (chat/add! name)
  (redirect "/chats"))

#_(defn -delete [id]
  (chat/delete! id)
  (redirect "/chats"))

#_(defn -clear [id]
  (chat/clear! id)
  (redirect-to id))
