(ns chats.handler
  (:require
    [chats.models.schema :as schema]
    [lobos.migrations]
    [lobos.migration :refer [list-migrations-names]]
    [lobos.connectivity :refer [open-global global-connections]]
    [lobos.core :only [migrate rollback print-pending print-done]]
    [ring.adapter.jetty :as jetty]
    [compojure.core :refer [defroutes routes]]
    [hiccup.bootstrap.middleware :refer [wrap-bootstrap-resources]]
    [compojure.handler :as handler]
    [compojure.route :as route]
    [chats.routes.home :refer [home-routes]]
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
    (println "migrations " @lobos.migration/migrations)
    (println "global-connections " @lobos.connectivity/global-connections)
    (println "open-global ...")
    (try (open-global schema/db-spec)
      (catch Exception e (str "caught exception: " (.getMessage e))))
    (println "global-connections " @lobos.connectivity/global-connections)
    (println "migrations " @lobos.migration/migrations)
    (println "pending migrations:" (with-out-str (lobos.core/print-pending)))
    (println "done migrations:"    (with-out-str (lobos.core/print-done)))

    (println "migrations " @lobos.migration/migrations)
    (println "pending migrations:" (with-out-str (lobos.core/print-pending)))
    (println "done migrations:"    (with-out-str (lobos.core/print-done)))
    (println "migrate ...")
    (lobos.core/migrate)
    (println "migrations " @lobos.migration/migrations)
    (println "pending migrations:" (with-out-str (lobos.core/print-pending)))
    (println "done migrations:"    (with-out-str (lobos.core/print-done)))
  )

(defn destroy []
    (timbre/info "chats is shutting down"))

(defroutes app-routes
  (route/resources "/")
  (route/not-found "Not Found"))

(def app
  (-> (routes home-routes app-routes)
      (handler/site)
      (wrap-bootstrap-resources)))

(defn -main [port]
  (println "main...")
  (migrate)
  (jetty/run-jetty app {:port (Integer. port) :join? false}))
