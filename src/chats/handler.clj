(ns chats.handler
  (:require
    [chats.models.schema :as schema]
    [chats.views.layout :as layout]
    [lobos.migrations]
    [lobos.migration :refer [list-migrations-names]]
    [lobos.connectivity :refer [open-global global-connections]]
    [lobos.core :only [migrate rollback print-pending print-done]]
    [ring.adapter.jetty :as jetty]
    [compojure.core :refer :all]
    [compojure.handler :as handler]
    [compojure.route :as route]
    [chats.routes.gui :as gui]
    [chats.routes.api :as api]
    [taoensso.timbre :as timbre]
    [com.postspectacular.rotor :as rotor])
  (:gen-class))


(defn info-appender [{:keys [level message]}]
    (println "level:" level "message:" message))

(defn init []
    (timbre/set-config!
          [:appenders :rotor]
          {:min-level :info
                :enabled? true
                :async? false ; should be always false for rotor
                :max-message-per-msecs nil
                :fn rotor/append})

    (timbre/set-config!
          [:shared-appender-config :rotor]
          {:path "chats.log" :max-size (* 512 1024) :backlog 10})

    (timbre/info "chats started successfully"))

(defn migrate []
    (println "migrate")
    (println "open-global ...")
    (try (open-global schema/db-spec)
      (catch Exception e (str "caught exception: " (.getMessage e))))
    (println "migrate ...")
    (lobos.core/migrate)
    (println "pending migrations:" (with-out-str (lobos.core/print-pending)))
    (println "done migrations:"    (with-out-str (lobos.core/print-done)))
  )

(defn destroy []
    (timbre/info "chats is shutting down"))

(defroutes app-routes
  (GET "/" [] (layout/common [:h1 "Welcome to chats!"]))

  (GET    "/chats"            []     (gui/all))
  (POST   "/chats"            [name] (gui/create name))
  (GET    "/chats/new"        []     (gui/new))

  (GET    "/chats/list"       []     (api/list))
  (GET    "/chats/rsp"        []     (api/get-rsp))
  (POST   "/chats/rsp"        []     (api/post-rsp))
  (GET    "/chats/req"        []     (api/get-req))
  (POST   "/chats/req"        []     (api/post-req))

  (GET    "/chats/:id"        [id]   (gui/show   (Integer/parseInt id)))
  (PUT    "/chats/:id/toggle" [id]   (gui/toggle (Integer/parseInt id)))
  (PUT    "/chats/:id/clear"  [id]   (gui/clear  (Integer/parseInt id)))
  (DELETE "/chats/:id"        [id]   (gui/delete (Integer/parseInt id)))

  (GET    "/chats/:id/query"  [id]   (api/query  (Integer/parseInt id)))
  (POST   "/chats/:id/delete" [id]   (api/delete (Integer/parseInt id)))
  (POST   "/chats/:id/create" [id]   (api/create (Integer/parseInt id)))
  (POST   "/chats/:id/init"   [id]   (api/init)  (Integer/parseInt id))

  (route/resources "/")
  (route/not-found "Not Found"))

(def app
  (-> (routes app-routes)
      (handler/site)))

(defn -main [port]
  (migrate)
  (jetty/run-jetty app {:port (Integer. port) :join? false}))
