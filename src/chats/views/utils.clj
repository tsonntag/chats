(ns chats.views.utils
  (:require 
    [clojure.string :refer [capitalize]]
    [hiccup.element :refer :all]
    [hiccup.form :refer :all]))

(defn prop-val [obj arg]
  (cond
    (vector? arg) (prop-val obj (second arg))
    (ifn? arg) (arg obj)
    :else arg))

(defn prop-key [arg]
   (cond
     (keyword? arg) (capitalize (name arg))
     (vector? arg)  (first arg)
     :else arg))

(defn table* [heads rows]
  [:table.table.table-striped
   [:thead
    [:tr
     (for [head heads]
       [:th head])]]
   [:tbody
    (for [row rows]
      [:tr
       (for [el row] 
         [:td el])])]])

(defn table [objs & cols]
  (let [heads (map prop-key cols)
        rows  (map (fn [obj] 
                     (map #(prop-val obj %) cols))
                   objs)]
    (table* heads rows)))
    

(defn prop-table [obj & args]
  (for [arg args]
    [:div.row
     [:strong.col-md-2 (str (prop-key arg) ":")]
     [:div.col-md-4    (prop-val obj arg)      ]]))

(defn control [field name text]
  [:div.row
   (label {:class "col-md-2"} name text)
   (field {:class "col-md-4"} name)])

(defn glyph [name title]
  [:span {:class (str "glyphicon glyphicon-" name)
          :title title}])

(defn icon-new    [& {:keys [title] :or {title "New"}}]    (glyph "plus"   title))
(defn icon-list   [& {:keys [title] :or {title "List"}}]   (glyph "list"   title))
(defn icon-delete [& {:keys [title] :or {title "Delete"}}] (glyph "remove" title))
(defn icon-clear  [& {:keys [title] :or {title "Clear"}}]  (glyph "minus"  title))
(defn icon-stop   [& {:keys [title] :or {title "Stop"}}]   (glyph "stop"   title))
(defn icon-play   [& {:keys [title] :or {title "Start"}}]  (glyph "play"   title))

(defn link-new [resource]
  (link-to (format "/%s/new" resource) (icon-new)))

(defn link-list [resource]
  (link-to (format "/%s" resource) (icon-list)))

(defn link-delete [resource id]
  (link-to {:rel "nofollow" :data-method "delete" :data-confirm "Are you sure?" }
     (format "/%s/%s" resource id) (icon-delete)))

(defmacro put-link [& args]
  `(link-to {:rel "nofollow" :data-method "put" :data-confirm "Are you sure?" }
            ~@args))

(defmacro links [resource & links]
  `(list 
    [:br]
    [:ul.list-inline
     ~@(for [link links]
        `[:li (-> ~resource ~link)])]
    [:br]))



