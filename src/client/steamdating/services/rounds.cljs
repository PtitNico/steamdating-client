(ns steamdating.services.rounds
  (:require [cljs.spec.alpha :as spec]
            [clojure.test.check.generators :as gen]
            [re-frame.core :as re-frame]
            [steamdating.models.player :as player]
            [steamdating.models.round :as round]
            [steamdating.services.db :as db]
            [steamdating.services.debug :as debug]
            [steamdating.models.filter :as filter]))


(db/reg-event-fx
  :sd.rounds.next/start
  [(re-frame/path [:tournament :players])]
  (fn next-start
    [{players :db} _]
    {:dispatch-n [[:sd.forms/reset :round (round/->round (player/on-board players))]
                  [:sd.routes/navigate "/rounds/next"]]}))


(db/reg-event-fx
  :sd.rounds.next/update-player
  [(re-frame/path [:forms :round :edit])]
  (fn update-edit-player
    [{form :db} [field name]]
    {:db (round/pair-player form field name)}))


(db/reg-event-fx
  :sd.rounds.next/suggest-sr-pairing
  (fn next-suggest-sr-pairing
    [{:keys [db]}]
    (let [players (gen/generate
                    (gen/shuffle
                      (player/on-board (get-in db [:tournament :players]))))
          rounds (get-in db [:tournament :rounds])
          settings (get-in db [:tournament :settings])]
      {:db (assoc-in db [:forms :round :edit :games]
                     (round/sr-pairing players rounds settings))})))


(db/reg-event-fx
  :sd.rounds.next/create
  (fn start-next
    [{:keys [db]} _]
    (let [new-round (get-in db [:forms :round :edit])]
      {:db (update-in db [:tournament :rounds] conj new-round)
       :dispatch [:sd.routes/navigate
                  (str "/rounds/nth/" (count (get-in db [:tournament :rounds])))]})))


;; (db/reg-event-fx
;;   :sd.rounds/rename-player
;;   [(re-frame/path [:tournament :rounds])]
;;   (fn rename-player
;;     [{rounds :db} [{{old-name :name} :base
;;                     {new-name :name} :edit}]]
;;     {:db (mapv #(round/rename-player % old-name new-name)
;;                rounds)}))


(db/reg-event-fx
  :sd.rounds.nth/random-score
  (fn random-score
    [{:keys [db]} [n-round]]
    (let [lists (player/lists (get-in db [:tournament :players]))]
      {:db (update-in db [:tournament :rounds n-round]
                      round/random-score lists)})))


(db/reg-event-fx
  :sd.rounds/drop-nth
  (fn drop-nth
    [{:keys [db]} [n]]
    (let [rounds (->> (get-in db [:tournament :rounds])
                      (round/drop-nth n))
          last-round-index (dec (count rounds))
          {page :page {page-round-index :n} :params} (get db :route)
          should-navigate? (and (= :rounds-nth page)
                                (> page-round-index last-round-index))
          navigate-to (if (= 0 (count rounds))
                        "/rounds/all"
                        (str "/rounds/nth/" last-round-index))]
      {:db (assoc-in db [:tournament :rounds] rounds)
       :dispatch (when should-navigate?
                   [:sd.routes/navigate navigate-to])})))


(defn rounds-sub
  [db [_ n]]
  {:pre [(spec/valid? :sd.db/db db)]
   :post [(spec/valid? :sd.round/rounds %)]}
  (get-in db [:tournament :rounds]))

(re-frame/reg-sub
  :sd.rounds/rounds
  rounds-sub)


(defn count-sub
  [rounds _]
  {:pre [(spec/valid? :sd.round/rounds rounds)]
   :post [(nat-int? %)]}
  (count rounds))

(re-frame/reg-sub
  :sd.rounds/count
  :<- [:sd.rounds/rounds]
  count-sub)


(defn opponents-sub
  [[rounds names]]
  {:pre [(spec/valid? :sd.round/rounds rounds)
         (spec/valid? :sd.player/names names)]
   :post [(spec/valid? :sd.round/opponents %)]}
  (round/opponents names rounds))

(re-frame/reg-sub
  :sd.rounds/opponents
  :<- [:sd.rounds/rounds]
  :<- [:sd.players/names]
  opponents-sub)


(defn next-sub
  [[form-state factions origins ;; rankings
    opponents icons]]
  {:pre [(spec/valid? :sd.form/form form-state)
         (spec/valid? :sd.player/factions factions)
         (spec/valid? :sd.player/origins origins)
         (spec/valid? :sd.round/opponents opponents)
         (spec/valid? :sd.faction/icons icons)]
   :post [(spec/valid? :sd.round/next %)]}
  (let [options (round/players-options (:edit form-state))]
    (-> form-state
        (round/validate-pairings {:factions factions
                                  :opponents opponents
                                  :origins origins})
        (assoc :factions factions
               :icons icons
               :options options))))

(re-frame/reg-sub
  :sd.rounds/next
  :<- [:sd.forms/validate :round round/validate]
  :<- [:sd.players/factions]
  :<- [:sd.players/origins]
  ;; :<- [:sd.rankings/players]
  :<- [:sd.rounds/opponents]
  :<- [:sd.factions/icons]
  next-sub)


(defn nth-raw-sub
  [db [_ n]]
  {:pre [(spec/valid? :sd.db/db db)
         (nat-int? n)]
   :post [(spec/valid? :sd.round/round %)]}
  (get-in db [:tournament :rounds n]))

(re-frame/reg-sub
  :sd.rounds/nth-raw
  nth-raw-sub)


(defn nth-filter-sub
  [[r f]]
  {:pre [(spec/valid? :sd.round/round r)
         (spec/valid? :sd.filter/value f)]
   :post [(spec/valid? :sd.round.nth/filter %)]}
  {:round (round/filter-with r (filter/->pattern f))
   :filter f})

(re-frame/reg-sub
  :sd.rounds/nth-filter
  (fn nth-filter-sub-input
    [[_ n filter]]
    [(re-frame/subscribe [:sd.rounds/nth-raw n])
     (re-frame/subscribe [:sd.filters/filter filter])])
  nth-filter-sub)


(defn nth-sort-sub
  [[{r :round :as input} s]]
  {:pre [(spec/valid? :sd.round.nth/filter input)
         (spec/valid? :sd.sort/sort s)]
   :post [(spec/valid? :sd.round.nth/sort %)]}
  (assoc input
         :round (round/sort-with r s)
         :sort s))

(re-frame/reg-sub
  :sd.rounds/nth-sort
  (fn nth-sort-sub-input
    [[_ n filter]]
    [(re-frame/subscribe [:sd.rounds/nth-filter n filter])
     (re-frame/subscribe [:sd.sorts/sort :round {:by :table}])])
  nth-sort-sub)


(defn nth-sub
  [[input factions icons] [_ n]]
  {:pre [(spec/valid? :sd.round.nth/sort input)
         (spec/valid? :sd.player/factions factions)
         (spec/valid? :sd.faction/icons icons)
         (nat-int? n)]
   :post [(spec/valid? :sd.round/nth %)]}
  (assoc input
         :n n
         :factions factions
         :icons icons))

(re-frame/reg-sub
  :sd.rounds/nth
  (fn nth-sub-input
    [[_ n filter]]
    [(re-frame/subscribe [:sd.rounds/nth-sort n filter])
     (re-frame/subscribe [:sd.players/factions])
     (re-frame/subscribe [:sd.factions/icons])])
  nth-sub)


;; (re-frame/reg-sub
;;   :sd.rounds/players-results
;;   :<- [:sd.rounds/rounds]
;;   :<- [:sd.rankings/ranking]
;;   :<- [:sd.filters/pattern :rounds-all]
;;   (fn players-results-sub
;;     [[rounds players pattern] _]
;;     (->> players
;;          (filter #(re-find pattern (:name %)))
;;          (mapv #(assoc %
;;                        :results (round/results-for-player (:name %) rounds)
;;                        :played-lists (round/lists-for-player (:name %) rounds))))))


;; (re-frame/reg-sub
;;   :sd.rounds/players-scores
;;   :<- [:sd.rounds/rounds]
;;   :<- [:sd.players/names]
;;   (fn players-scores-sub
;;     [[rounds names] _]
;;     (round/total-scores-for-players names rounds)))


;; (re-frame/reg-sub
;;   :sd.rounds/summary
;;   :<- [:sd.rounds/players-results]
;;   :<- [:sd.sorts/sort :rounds-all {:by [:name]}]
;;   (fn summary-sub
;;     [[results {:keys [by reverse]}] _]
;;     (as-> results $
;;       (sort-by #(let [value (get-in % by)]
;;                   (if (string? value)
;;                     (.toLowerCase value)
;;                     value)) $)
;;       (cond-> $
;;         reverse clojure.core/reverse))))
