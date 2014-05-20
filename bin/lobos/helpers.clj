(ns lobos.helpers
    (:refer-clojure :exclude [bigint boolean char double float time])
    (:use (lobos schema)))

(defn surrogate-key [table]
  (integer table :id :auto-inc :primary-key))

(defn timestamps [table]
  (-> table
      (timestamp :created-at (default (now)))
      (timestamp :updated-at)))

