(ns chats.routes.api
  (:require
    [clojure.string :as str]
    [compojure.core :refer :all]
    [chats.models.chat :refer :all]
    [taoensso.timbre :refer [info debug]]))

(defn item-s [item]
  (into {}
        (map second
             (select-keys item [:request :response :responded-at]))))

(defn- render [& {:keys [status item] :or {status 200 item {}} :as arg}]
  (let [data (filter second
                     (assoc (dissoc arg :status :item)
                     :item (:id item)
                     :req  (:request item)
                     :rsp  (:response item)))
        body  (str/join "" (for [[k v] data] (format "%s=%s\n" (name k) v)))]
    (info "render" (into {:status status} data))
    {:status status :body body}))

(defmacro with-chat [chat name active & body]
  `(if-let [~chat (if ~active
                    (find-chat :name ~name :finished-at nil)
                    (find-chat :name ~name))]
     (do ~@body)
     (render :status 404
             :chat ~name
             :msg "not found")))

(defn list-chats []
  (let [lines (map #(format "%-10s: %4d\n" (:name %) (item-count %)) (chats))
        head  "#chat items\n"
        body  (apply str (cons head lines))]
    {:status 200 :body  body}))

(defn query [name]
  (with-chat chat name false
     (let [fmt    "%s;%s;%s;%s;%s\n"
           head   ["#item" "created" "request" "response" "response_forwarded"]
           rows   (sort-by :created-at (map #((juxt :id :created-at :request :response :responded-at) %) (:chat-item chat)))
           lines  (map #(apply format fmt %) (cons head rows))
           title  (format "chat: %s\n" name)
           body   (apply str (cons title lines))]
       {:status 200 :body body})))

(defn delete [name]
  (with-chat chat name false
    (chat-delete! (:id chat))
    (render :chat name
            :msg "deleted")))

(defn- -create [name force]
  (if-let [chat (find-chat :name name)]
    (if force
      (do
        (chat-clear! (:id chat))
        (render :chat name
                :msg "created"))
      (render :status 400
              :chat name
              :msg "already exists"))
    (if-let [chat (chat-create! name)]
      (render :chat name
              :msg "created")
      (render :status 400
              :chat name
              :msg "create failed"))))

(defn create [name]
  (-create name false))

(defn init [name]
  (-create name true))

(defn- forward-response [name item]
  (item-responded! item)
  (render :chat name
          :item item))

(defn- poll [tries msecs f]
  (when (> tries 0)
    (if-let [result (f)]
      (do
        (debug "poll: returning" result)
        result)
      (do
        (Thread/sleep msecs)
        (recur (dec tries) msecs f)))))

(defn- poll-rsp [name item]
  (info name "poll-rsp" item)
  (if-let [item (poll 50 200 #(find-item item (responded)))]
    (forward-response name item)
    (render :status 404
            :chat name
            :item item
            :msg "Timeout")))

(defn get-req [name]
  (with-chat chat name true
    (info "get-req:" name "polling for request..")
    (if-let [item (poll 25 200 #(first (items (for-chat chat) (not-responded))))]
      (render :chat name
              :item item)
      (render :status 404
              :chat name
              :msg "Timeout"))))

(defn get-rsp [name]
  (with-chat chat name true
    (info name "get-rsp")
    (if-let [item (first (items (for-chat chat) (responded) (response-not-forwarded)))]
      (forward-response name item)
      (if-let [item (first (items (for-chat chat) (not-responded)))]
        (poll-rsp name item)
        (render :status 404
                :chat name
                :msg "no open req")))))

(defn post-req [name req]
  (with-chat chat name true
    (info name "post-req")
    (doseq [item (items (for-chat chat) (not-responded))]
      (do
        (info name "post-req" req ". marking obsolete not responded request" item)
        (item-obsolete! item)))

    (doseq [item (items (for-chat chat) (response-not-forwarded))]
      (do
        (info name "post-req" req ". marking obsolete not forwarded response for request" item)
        (item-obsolete! item)))

    (let [item (add-item! {:chat_id (:id chat) :request req})]
      (info name "post-req. created request" req ". waiting for response...")
      (poll-rsp name item))))

(defn post-rsp [name rsp item-id]
  (with-chat chat name true
    (info name "post-rsp")
    (if-let [item (find-item :id item-id)]
      (if (:response item)
        (render :status 400
                :chat name
                :item item
                :msg "item has already a rsp")
        (do
          (item-response! item-id rsp)
          (render :chat name
                  :item (assoc item :response rsp)
                  :msg "rsp accepted")))
      (render :status 404
              :chat name
              :item {:id item-id}
              :msg (str "no item " item-id)))))


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

