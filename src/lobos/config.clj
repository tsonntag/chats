(ns lobos.config
  (:use lobos.connectivity)
  (:require [chats.models.db :as db]))

(open-global db/db)
