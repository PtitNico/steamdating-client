(ns steamdating.pages.players-list
  (:require [re-frame.core :as re-frame]
            [secretary.core :refer [defroute]]
            [steamdating.components.generics.icon :refer [icon]]
            [steamdating.components.page.content :refer [page-content]]
            [steamdating.components.page.menu :refer [page-menu-items]]
            [steamdating.services.db :as db]
            [steamdating.services.debug :as debug]
            [steamdating.services.players]))


(defroute players-list "/players" {}
  (re-frame/dispatch [:sd.routes/page :players-list]))


(defmethod page-content :players-list
  []
  [:div.sd-page-players-list
   [:h4 "Players"]])


(defmethod page-menu-items :players-list
  []
  (list
    [:button.item
     {:key :create
      :on-click #(re-frame/dispatch [:sd.players/start-create])}
     [:span "Create player "]
     [icon {:name "user-plus"}]]))
