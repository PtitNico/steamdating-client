(ns steamdating.components.online.online-follow
  (:require [cljsjs.kjua]
            [re-frame.core :as re-frame]
            [steamdating.components.generics.icon :refer [icon]]
            [steamdating.services.debug :as debug]))


(defn render-qr-code
  [url ref]
  (when ref
    (aset ref "innerHTML" "")
    (let [rect (.getBoundingClientRect ref)
          width (aget rect "width")
          qr-element (js/kjua (clj->js {:text url
                                        :ecLevel "M"
                                        :size (- width 10)}))]
      ;; (js/console.log
      ;;   "render-qr-code"
      ;;   ref rect qr-element)
      (.appendChild ref qr-element))))


(defn online-follow
  []
  (let [{:keys [show? url name]} @(re-frame/subscribe [:steamdating.online/follow-status])]
    [:div.sd-OnlineFollow
     {:class (when show? "sd-OnlineFollow-show")
      :on-click #(re-frame/dispatch [:steamdating.online/toggle-show-follow])}
     [:div.sd-OnlineFollow-mask
      [:div.sd-OnlineFollow-content
       [:h4 (str "Follow " name)]
       [:p
        [:a.sd-OnlineFollow-link
         {:href url} url]]
       [:div.sd-OnlineFollow-qrCode
        {:ref #(render-qr-code url (when show? %))}]
       [:button.sd-OnlineFollow-close
        {:type :button}
        [icon "x"] " Close"]]]]))


(defn online-follow-toggle
  []
  (let [{:keys [name status]} @(re-frame/subscribe [:steamdating.online/follow-status])]
    (when (= :synced status)
      [:button.sd-OnlineFollow-toggle
       {:type :button
        :on-click #(re-frame/dispatch [:steamdating.online/toggle-show-follow])}
       [:img.sd-OnlineFollow-qr-code-icon
        {:src "/icons/qr-code.png"}]
       [:span.sd-OnlineFollow-toggle-text
        "Follow " name " online"]])))
