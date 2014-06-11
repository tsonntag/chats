(ns chats.handler
  (:require
    [chats.models.schema :as schema]
    [chats.views.layout :as layout]
    [lobos.migrations]
    [lobos.connectivity :refer [open-global]]
    [lobos.core :only [migrate rollback print-pending print-done]]
    [ring.adapter.jetty :as jetty]
    [compojure.core :refer :all]
    [compojure.handler :as handler]
    [compojure.route :as route]
    [chats.routes.gui :refer [gui-routes]]
    [chats.routes.api :refer [api-routes]]
    [taoensso.timbre :refer [info debug set-config!]]
    [clojure.pprint :refer [pprint]]
    [com.postspectacular.rotor :as rotor])
  (:gen-class))

(defn info-appender [{:keys [level message]}]
    (println "level:" level "message:" message))

(defn init []
    (set-config!
          [:appenders :rotor]
          {:min-level :info
                :enabled? true
                :async? false ; should be always false for rotor
                :max-message-per-msecs nil
                :fn rotor/append})

    (set-config!
          [:shared-appender-config :rotor]
          {:path "chats.log" :max-size (* 512 1024) :backlog 10})

    (info "chats started successfully"))

(defn migrate []
    (info "migrate")
    (info "open-global ...")
    (try (open-global schema/db-spec)
      (catch Exception e (str "caught exception: " (.getMessage e))))
    (info "migrate ...")
    (lobos.core/migrate)
    ;(info "pending migrations:" (with-out-str (lobos.core/print-pending)))
    ;(info "done migrations:"    (with-out-str (lobos.core/print-done)))
  )

(defn destroy []
    (info "chats is shutting down"))

(defn with-header [handler header value]
  (fn [request]
    (let [response (handler request)]
      (assoc-in response [:headers header] value))))

(defn wrap-no-cache [handler]
  (with-header handler "Cache-Control" "max-age=0, must-revalidate"))

(defroutes home-routes
  (GET "/" [] (layout/common [:h1 "Welcome to chats!"])))

(defroutes app-routes
  (route/resources "/")
  (route/not-found "Not Found"))

(def app
  (wrap-no-cache
    (-> (routes
         home-routes
         api-routes
         gui-routes
         app-routes)
        (handler/site))))

(defn -main [port]
  (migrate)
  (jetty/run-jetty app {:port (Integer. port) :join? false}))
