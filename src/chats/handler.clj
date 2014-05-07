(ns chats.handler
  (:require
    [lobos.core :only [migrate]]
    [ring.adapter.jetty :as jetty]
    [compojure.core :refer [defroutes routes]]
    [ring.middleware.resource :refer [wrap-resource]]
    [ring.middleware.file-info :refer [wrap-file-info]]
    [hiccup.middleware :refer [wrap-base-url]]
    [compojure.handler :as handler]
    [compojure.route :as route]
    [chats.routes.home :refer [home-routes]]
    [taoensso.timbre :as timbre]
    [com.postspectacular.rotor :as rotor]))

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
          {:path "error.log" :max-size (* 512 1024) :backlog 10})

    (timbre/info "chats started successfully"))

(defn destroy []
    (timbre/info "chats is shutting down"))

(defroutes app-routes
  (route/resources "/")
  (route/not-found "Not Found"))

(def app
  (-> (routes home-routes app-routes)
      (handler/site)
      (wrap-base-url)))

(defn -main [port]
  (lobos.core/migrate)
  (jetty/run-jetty app {:port (Integer. port) :join? false}))
