(ns chats.routes.chat
  (:refer-clojure :exclude [list])
  (:require
    [clojure.string :as str]
    [compojure.core :refer :all]
    [chats.views.layout :as layout]
    [chats.models.chat :as chat]
    [chats.views.utils :refer :all]
    [noir.response :refer [redirect]]
    [taoensso.timbre :refer [info debug]]
    [hiccup.element :refer :all]
    [hiccup.form :refer :all]))

;;;;;;;;;;;;;;;;;;;;; api ;;;;;;;;;;;;;;;;;;;;;;
(defn- link [chat]
  (link-to (format "/chats/%s" (:id chat)) (:name chat)))

(defn- link-toggle [_ chat]
  (let [icon (if (chat/active? chat)
               (icon-stop :title "Deactivate")
               (icon-play :title "Activate"))]
    (put-link (format "/chats/%s/toggle" (:id chat)) icon)))

(defn- link-clear [_ id]
   (put-link (format "/chats/%s/clear" id) (icon-clear :title "Delete items")))

(defn- redirect-to [id]
  (redirect (str "/chats/" id)))

(defn all
  ([chats]
    (layout/common
      [:h1 "Chats"]
      (links "chats" link-new)
      (table chats
             ["Name" link]
             :created-at
             ["Active" chat/active?]
             ["Items"  chat/item-count])))
  ([]
   (all (chat/all))))

(defn show [id]
  (if-let [chat (chat/find :id id)]
    (layout/common
      [:h1 "Chat"]
      (links "chats" link-list (link-delete id) (link-clear id) (link-toggle chat))
      (prop-table chat
                  :name
                  :created-at
                  ["Active" (chat/active? chat)])
      [:br]
      (table (:chat-item chat)
             :id
             :created-at
             :request
             :response
             ["Response forwarded" :responded-at]))
    {:status 404
     :body (format "chat=%s\nmsg=not found\n" id)}))

(defn new-chat []
   (layout/common
     [:h1 "Create Chat"]
     [:br]
     (form-to [:post "/chats"]
              (control text-field :name "Name:")
              [:br]
              (submit-button "Create"))))

(defn create [name]
  (chat/create! name)
  (redirect "/chats"))

(defn delete [id]
  (chat/delete! id)
  (redirect "/chats"))

(defn toggle [id]
  (chat/toggle-active! id)
  (redirect-to id))

(defn clear [id]
  (chat/clear! id)
  (redirect-to id))


;;;;;;;;;;;;;;;;;;;;; api ;;;;;;;;;;;;;;;;;;;;;;

(defn- render [& {:keys [status item] :or {status 200 item {}} :as arg}]
  (let [hash  (assoc (dissoc arg :status :item)
                     :item (:id item)
                     :req  (:request item)
                     :rsp  (:response item))
        body  (str/join "" (for [[k v] (filter second hash)] (format "%s=%s\n" (name k) v)))]
    {:status status :body body}))

(defmacro with-chat [chat name active & body]
  `(if-let [~chat (if ~active 
                    (chat/find :name ~name :finished-at nil)
                    (chat/find :name ~name))]
     (do ~@body)
     (render :status 404
             :chat ~name
             :msg "not found")))

(defn list []
  (let [lines (map #(format "%-10s: %4d\n" (:name %) (chat/item-count %)) (chat/all))
        head  "#chat items\n"
        body  (apply str (cons head lines))]
    {:status 200 :body  body}))

(defn query [name]
  (with-chat chat name false
     (let [fmt    "%s;%s;%s;%s;%s\n"
           head   ["#item" "created" "request" "response" "response_forwarded"] 
           rows   (sort-by :created-at (map #((juxt :id :created-at :request :response :responded-at) %) (:chat-item chat)))
           lines  (map #(apply format fmt %) (cons head rows))
           title  (format "chat: %s\n" (:name chat))
           body   (apply str (cons title lines))]
       {:status 200 :body body})))

(defn delete [name]
  (with-chat chat name false
    (chat/delete! (:id chat))
    (info "deleted " name))
    (render :chat name
            :msg "deleted"))

(defn- -create [name force]
  (if-let [chat (chat/find :name name)]
    (if force
      (do
        (chat/clear!)
        (info "cleared " name)
        (render :chat name
                :msg "created"))
      (render :status 400
              :chat name
              :msg "already exists"))
    (if-let [chat (chat/create! name)]
      (do 
        (info "created " name)
        (render :chat name
                :msg "created"))
      (render :status 400
              :chat name
              :msg "create failed"))))

(defn create [name]
  (-create name false))

(defn init [id]
  (-create name true))

(defn- forward-response [name item]
  (chat/item-responded! item)
  (render :chat name
          :item item))

(defn- poll [tries msecs f]
  (when (> tries 0)
    (if-let [result (f)]
      (do
        (info "poll: returning " result)
        result)
      (do 
        (Thread/sleep msecs)
        (recur (dec tries) msecs f)))))

(defn- poll-rsp [name item]
  (info "polling response for chat " name " item " (:id item) " " (:request item))
  (if-let [item (poll 50 200 #(chat/find-item :id (:id item) :response [not= nil]))]
    (forward-response name item)
    (render :status 404
            :chat name
            :item item
            :msg "Timeout")))

(defn get-req [name]
  (with-chat chat name true
    (info "get-req: polling for request for" name)
    (if-let [item (poll 25 200 #(first (chat/not-responded-items chat)))]
      (render :chat name
              :item item)
      (render :status 404 
              :chat name
              :msg "Timeout"))))

(defn get-rsp [name]
  (with-chat chat name true
    (info "get-rsp chat: " name)
    (if-let [item (first (filter :response (chat/response-not-forwarded-items chat)))]
      (forward-response name item)
      (if-let [item (first (chat/not-responded-items chat))]
        (poll-rsp name item)
        (render :status 404
                :chat name
                :msg "no open req")))))

(defn post-req [name req]
  (with-chat chat name true
    (doseq [item (chat/not-responded-items chat)]
      (do
        (info "post-req " req " for chat " (:name chat) ". marking obsolet not responded request " (:request item))
        (chat/item-obsolete! item)))

    (doseq [item (chat/response-not-forwarded-items chat)]
      (do
        (info "post-req " req " for chat " (:name chat) ". marking obsolet not forwarded response for request " (:request item))
        (chat/item-obsolete! item)))

    (let [item (chat/add-item! {:chat_id (:id chat) :request req})]
      (info "post-req: created request " req ". waiting for response...")
      (poll-rsp name item))))

(defn post-rsp [name rsp item-id]
  (with-chat chat name true
    (if-let [item (chat/find-item :id item-id)]
      (if (:response item)
        (render :status 400
                :chat name
                :item item
                :msg "item has already a rsp")
        (do
          (chat/item-response! item-id rsp)
          (info "post-rsp: " rsp " for " name " accepted")
          (render :chat name
                  :item (assoc item :response rsp)
                  :msg "rsp accepted")))
      (render :status 404
              :chat name
              :item {:id item-id}
              :msg (str "no item " item-id)))))


(defroutes chat-routes
    ; gui
    (GET    "/chats"            []     (all))
    (POST   "/chats"            [name] (create name))
    (GET    "/chats/new"        []     (new-chat))
    (GET    "/chats/:id"        [id]   (show   (Integer/parseInt id)))
    (PUT    "/chats/:id/toggle" [id]   (toggle (Integer/parseInt id)))
    (PUT    "/chats/:id/clear"  [id]   (clear  (Integer/parseInt id)))
    (DELETE "/chats/:id"        [id]   (delete (Integer/parseInt id)))

    ; api
    (GET    "/chats/list"         []              (list))
    (GET    "/chats/:name/rsp"    [name]          (get-rsp  name))
    (POST   "/chats/:name/rsp"    [name rsp item] (post-rsp name rsp (Integer/parseInt item)))
    (GET    "/chats/:name/req"    [name]          (get-req  name))
    (POST   "/chats/:name/req"    [name req]      (post-req name req))
    (GET    "/chats/:name/query"  [name]          (query    name))
    (POST   "/chats/:name/delete" [name]          (delete   name))
    (POST   "/chats/:name/create" [name]          (create   name))
    (POST   "/chats/:name/init"   [name]          (init     name))
  )

