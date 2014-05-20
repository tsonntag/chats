(ns chats.routes.chat
  (refer-clojure :exclude [list new])
  (:require
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
     :body (format "chat=%s\nmsg=Not found\n" id)}))

(defn new []
   (layout/common
     [:h1 "Create Chat"]
     [:br]
     (form-to [:post "/chats"]
              (control text-field :name "Name:")
              [:br]
              (submit-button "Create"))))

(defn create [name]
  (chat/add! name)
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

(defn render [& {:keys [status] :or {status 200} :as arg}]
  (let [hash (dissoc arg :status)
        body (str/join "\n" (for [[k v] hash] (format "%s=%s" (name k) v)))]
    {:status status :body body}))

(defmacro with-chat [chat name & body]
  `(if-let [~chat (chat/find :name ~name)]
     (do ~@body)
     (render :status 404 :chat ~name :msg "Not found")))

(defmacro with-active-chat [chat & body]
  `(if-let [~chat (chat/find-active)]
     (do ~@body)
     (render :status 404 :msg "Not found")))

(defn list []
  (let [lines (map #(format "%-10s: %4d\n" (:name %) (chat/item-count %)) (chat/all))
        head  "#chat items\n"
        body  (apply str (cons head lines))]
    {:status 200 :body  body}))

(defn query [name]
  (with-chat chat name
     (let [fmt    "%s;%s;%s;%s;%s\n"
           head   ["#item" "created" "request" "response" "response_forwarded"] 
           rows   (sort-by :created-at (map #((juxt :id :created-at :request :response :responded-at) %) (:chat-item chat)))
           lines  (map #(apply format fmt %) (cons head rows))
           title  (format "chat: %s\n" (:name chat))
           body   (apply str (cons title lines))]
       {:status 200 :body body})))

(defn delete [name]
  (with-chat chat name
    (chat/delete! (:id chat))
    (render :chat name)
    (info "deleted " name)))

(defn- -create [name force]
  (if-let [chat (chat/find :name name)]
    (if force
      (do
        (chat/clear!)
        (info "cleared " name)
        (render :msg "created")
      (render :status 400
              :chat name
              :msg "already exists"))
    (if-let [chat (chat/create! name)]
      (do 
        (info "created " name))
        (render :msg "created")
      (render :status 400
              :chat name
              :msg "create failed"))))

(defn create [name]
  (-create name false))

(defn init [id])
  (-create name true))

(defn- forward-response [item]
  (chat/item-responded! item)
  (render :item item))

(defn- poll
  ([tries msecs f]
   (when (> tries 0)
     (if-let [result (f)]
       result
       (do 
         (Thread/sleep msecs)
         (recur (dec tries msecs) f)))))
  ([secs f]
   (let [dsec  0.2
         tries (int (/ secs 0.2))]
     (poll tries (* 1000 dsec)))))

(defn- poll-rsp [item]
  (info "polling reponse for " (:id item) " " (:request item))
  (if-let [item (poll 10 #(chat/find-item :id (:id item)))]
    (forward-response item)
    (render :status 404
            :item item
            :msg "Timeout")))

(defn get-req []
  (with-active-chat chat
    (info "get-req: polling for request for" (:name chat))
    (if-let [item (poll 5.0 #(first (chat/not-responded-items chat)))]
      (render :item item)
      (render :status 404 
              :msg "Timeout"))))

(defn get-rsp []
  (with-active-chat chat
    (info "get-rsp chat: " (:name chat))
    (if-let [item (chat/response-not-forwarded-items chat)]
      (forward-response item)
      (if-let [item (first (chat/not-responded-items chat))]
        (poll-rsp item)
        (render :status 404
                :msg "No open req")))))

(defn post-req [req])
  (with-active-chat chat
    (doseq [item (chat/not-responded-items chat)]
      (do
        (info "post-req " req " for chat " (:name chat) ". marking obsolet not responded request " (:request item))
        (chat/obsolet-item! (:id item))))

    (doseq [item (chat/response-not-forwarded-items chat)]
      (do
        (info "post-req " req " for chat " (:name chat) ". marking obsolet not forwarded response for request " (:request item))
        (chat/obsolet-item! (:id item))))

    (let [item (chat/create-item! (:id chat) :request req)]
      (info "post-req: created request " req ". waiting for response...")
      (poll-rsp item))))

(defn post-rsp [rsp item-id]
  (with-active-chat chat
    (if-let [item (chat/find-item item-id)]
      (if (:response item)
        (render :status 400
                :item item
                :msg "Item has already a rsp")
        (do
          (chat/respond-item! item-id rsp)
          (info "pos-rsp: " rsp " for " (:name chat) " accepted")
          (render :item item
                  :msg "Rsp accepted")))
      (render :status 404
              :item {:id item-id}
              :msg (str "No item " item-id)))))


(defroutes app-routes
    ; gui
    (GET    "/chats"            []     (all))
    (POST   "/chats"            [name] (create name))
    (GET    "/chats/new"        []     (new))

    ; api
    (GET    "/chats/list"       []          (list))
    (GET    "/chats/rsp"        []          (get-rsp))
    (POST   "/chats/rsp"        [rsp item]  (post-rsp rsp item))
    (GET    "/chats/req"        []          (get-req))
    (POST   "/chats/req"        [req]       (post-req req))

    ; gui
    (GET    "/chats/:id"        [id]   (show   (Integer/parseInt id)))
    (PUT    "/chats/:id/toggle" [id]   (toggle (Integer/parseInt id)))
    (PUT    "/chats/:id/clear"  [id]   (clear  (Integer/parseInt id)))
    (DELETE "/chats/:id"        [id]   (delete (Integer/parseInt id)))

    ; api
    (GET    "/chats/:name/query"  [name] (query  name))
    (POST   "/chats/:name/delete" [name] (delete name)))
    (POST   "/chats/:name/create" [name] (create name))
    (POST   "/chats/:name/init"   [name] (init   name))
  )

