(ns steamdating.services.toaster
  (:require [cljs.spec.alpha :as spec]
            [re-frame.core :as re-frame]
            [steamdating.models.toaster]
            [steamdating.services.db :as db]))


(defonce timeout
  (atom nil))


(re-frame/reg-fx
  :sd.toaster/timeout
  (fn []
    (when @timeout (js/clearTimeout @timeout))
    (reset! timeout (js/setTimeout
                      #(re-frame/dispatch
                         [:sd.toaster/clear]) 1000))))


(db/reg-event-fx
  :sd.toaster/set
  [(re-frame/path :toaster)]
  (fn set
    [_ [toaster]]
    {:db toaster
     :sd.toaster/timeout nil}))


(db/reg-event-fx
  :sd.toaster/clear
  (fn clear
    [{:keys [db]}]
    {:db (dissoc db :toaster)}))


(defn toaster-sub
  [db]
  {:pre [(spec/valid? :sd.db/db db)]
   :post [(spec/valid? (spec/nilable :sd.toaster/toaster) %)]}
  (:toaster db))

(re-frame/reg-sub
  :sd.toaster/toaster
  toaster-sub)
