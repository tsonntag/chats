(ns chats.routes.api
  (:require
    [clojure.string :as str]
    [compojure.core :refer :all]
    [chats.models.chat :refer :all]
    [ring.util.response :refer [response]]
    [taoensso.timbre :refer [info debug]]))

(defn item-s [item]
  (into {}
        (map second
             (select-keys item [:request :response :responded-at]))))


(defn- render [title & {:keys [status item] :or {status 200 item {}} :as arg}]
  (let [data (filter second
                     (assoc (dissoc arg :status :item)
                     :item (:id item)
                     :req  (:request item)
                     :rsp  (:response item)))
        body  (str/join "" (for [[k v] data] (format "%s=%s\n" (name k) v)))]
    (info title "render" (into {:status status} data))
    {:status status :body body}))

(defmacro with-chat [chat name active & body]
  `(if-let [~chat (if ~active
                    (find-chat :name ~name :finished-at nil)
                    (find-chat :name ~name))]
     (do ~@body)
     (render "with-chat" :status 404
             :chat ~name
             :msg "not found")))

(defn list-chats []
  (let [lines (map #(format "%-10s: %4d\n" (:name %) (item-count %)) (chats))
        head  "#chat items\n"
        body  (apply str (cons head lines))]
    (response body)))

(defn query [name]
  (with-chat chat name false
     (let [fmt    "%s;%s;%s;%s;%s\n"
           head   ["#item" "created" "request" "response" "response_forwarded"]
           rows   (sort-by :created-at (map #((juxt :id :created-at :request :response :responded-at) %) (:chat-item chat)))
           lines  (map #(apply format fmt %) (cons head rows))
           title  (format "chat: %s\n" name)
           body   (apply str (cons title lines))]
    (response body))))

(defn delete [name]
  (with-chat chat name false
    (chat-delete! (:id chat))
    (render "delete:"
            :chat name
            :msg "deleted")))

(defn- -create [name force]
  (let [title "create:"]
    (if-let [chat (find-chat :name name)]
      (if force
        (do
          (chat-clear! (:id chat))
          (render title
                  :chat name
                  :msg "created"))
        (render title
                :status 400
                :chat name
                :msg "already exists"))
      (if-let [chat (chat-create! name)]
        (render title
                :chat name
                :msg "created")
        (render title
                :status 400
                :chat name
                :msg "create failed")))))

(defn create [name]
  (-create name false))

(defn init [name]
  (-create name true))

(defn- forward-response [name item-id title]
  (let [item (item-responded! item-id)]
    (render title
            :chat name
            :item item)))

(defn- poll [tries msecs f]
  (when (> tries 0)
    (if-let [result (f)]
      (do
        (debug "poll: returning" result)
        result)
      (do
        (Thread/sleep msecs)
        (recur (dec tries) msecs f)))))

(defn- poll-rsp [name item title]
  (info title name item "poll-rsp...")
  (if-let [item (poll 50 200 #(find-item item (responded)))]
    (forward-response name (:id item) title)
    (render title
            :status 404
            :chat name
            :item item
            :msg "Timeout")))

(defn get-req [name]
  (let [title "get-req:"]
    (with-chat chat name true
      (info title name "polling for request..")
      (if-let [item (poll 25 200 #(first (items (for-chat chat) (not-responded))))]
        (render title
                :chat name
                :item item)
        (render title
                :status 404
                :chat name
                :msg "Timeout")))))

(defn get-rsp [name]
  (let [title "post-req:"]
    (with-chat chat name true
      (info title name)
      (if-let [item (first (items (for-chat chat) (responded) (response-not-forwarded)))]
        (forward-response name (:id item) title)
        (if-let [item (first (items (for-chat chat) (not-responded)))]
          (poll-rsp name item title)
          (render title
                  :status 404
                  :chat name
                  :msg "no open req"))))))

(defn post-req [name req]
  (let [title "post-req:"]
    (with-chat chat name true
      (info title name)
      (doseq [item (items (for-chat chat) (not-responded))]
        (do
          (info title name req ". marking obsolete not responded request" item)
          (item-obsolete! item)))

      (doseq [item (items (for-chat chat) (response-not-forwarded))]
        (do
          (info title name req ". marking obsolete not forwarded response for request" item)
          (item-obsolete! item)))

      (let [item (add-item! {:chat_id (:id chat) :request req})]
        (info title name "created request" req ". waiting for response...")
        (poll-rsp name item title)))))

(defn post-rsp [name rsp item-id]
  (let [title "post-rsp:"]
    (with-chat chat name true
      (info title name)
      (if-let [item (find-item {:id item-id})]
        (if (:response item)
          (render title
                  :status 400
                  :chat name
                  :item item
                  :msg "item has already a rsp")
          (do
            (item-response! item-id rsp)
            (render title
                    :chat name
                    :item (assoc item :response rsp)
                    :msg "rsp accepted")))
        (render title
                :status 404
                :chat name
                :item {:id item-id}
                :msg (str "no item " item-id))))))


(defroutes api-routes
    (GET    "/chats/list"         []              (list-chats))
    (GET    "/chats/:name/rsp"    [name]          (get-rsp  name))
    (POST   "/chats/:name/rsp"    [name rsp item] (post-rsp name rsp (Integer/parseInt item)))
    (GET    "/chats/:name/req"    [name]          (get-req  name))
    (POST   "/chats/:name/req"    [name req]      (post-req name req))
    (GET    "/chats/:name/query"  [name]          (query    name))
    (POST   "/chats/:name/delete" [name]          (delete   name))
    (POST   "/chats/:name/create" [name]          (create   name))
    (POST   "/chats/:name/init"   [name]          (init     name))
  )

