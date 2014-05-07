(ns lobos.helpers
  (:require
    [lobos.schema    :refer :all]))

(defn surrogate-key [table]
  (integer table :id :auto-inc :primary-jey))

(defn timestamps [table]
  (-> table
      (timestamp :created-at (default (now)))
      (timestamp :updated-at)))

