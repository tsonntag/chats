(defproject chats "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [compojure "1.1.6"]
                 [hiccup "1.0.5"]
                 [ring-server "0.3.1"]
                 [ring/ring-jetty-adapter "1.3.0-beta1"]
                 [postgresql/postgresql "9.1-901.jdbc4"]
                 [korma "0.3.1"]
                 ;[org.clojure/java.jdbc "0.1.1"]
                 [lobos "1.0.0-beta1"]
                 [log4j "1.2.15"
                     :exclusions [javax.mail/mail
                                  javax.jms/jms
                                  com.sun.jdmk/jmxtools
                                  com.sun.jmx/jmxri]]
                 [com.taoensso/timbre "3.1.6"]
                 [com.postspectacular/rotor "0.1.0"]
                 [lib-noir "0.8.2"]
                 [ring-middleware-format "0.3.2"]]

  :plugins [[lein-ring "0.8.10"]]
  :ring {:handler chats.handler/app
         :init chats.handler/init
         :destroy chats.handler/destroy}
  ;:aot :all
  :uberjar-name "chats-standalone.jar"
  :min-lein-version "2.0.0"
  :profiles
  {:production
   {:ring
    {:open-browser? false, :stacktraces? false, :auto-reload? false}}
   :dev
   {:dependencies [[ring-mock "0.1.5"]
                   [ring/ring-devel "1.2.1"]
                   ;[org.clojure/java.classpath "0.2.2"]
                   ;[org.clojure/java.jdbc "0.1.1"]
                   ]}})
