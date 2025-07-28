(ns jmshelby.monopoly-web.events
  (:require
   [re-frame.core :as re-frame]
   [jmshelby.monopoly-web.db :as db]
   ))

(re-frame/reg-event-db
 ::initialize-db
 (fn [_ _]
   db/default-db))

(re-frame/reg-event-fx
  ::navigate
  (fn [_ [_ handler]]
   {:navigate handler}))

(re-frame/reg-event-fx
 ::set-active-panel
 (fn [{:keys [db]} [_ active-panel]]
   {:db (assoc db :active-panel active-panel)}))

;; Game setup events
(re-frame/reg-event-db
 ::set-game-mode
 (fn [db [_ mode]]
   (assoc-in db [:game-setup :mode] mode)))

(re-frame/reg-event-db
 ::update-players
 (fn [db [_ players]]
   (assoc-in db [:game-setup :players] players)))

;; Single game events  
(re-frame/reg-event-db
 ::set-single-game-state
 (fn [db [_ game-state]]
   (assoc-in db [:single-game :state] game-state)))

(re-frame/reg-event-fx
 ::start-single-game
 (fn [{:keys [db]} _]
   (let [players (get-in db [:game-setup :players])]
     {:db (-> db
              (assoc-in [:single-game :running?] true)
              (assoc-in [:single-game :log] [])
              (assoc-in [:single-game :state] nil))
      :dispatch [::create-game (count players)]})))

(re-frame/reg-event-fx
 ::stop-single-game
 (fn [{:keys [db]} _]
   {:db (assoc-in db [:single-game :running?] false)
    :dispatch [::add-game-log-entry "Game stopped by user"]}))

(re-frame/reg-event-fx
 ::create-game
 (fn [{:keys [db]} [_ player-count]]
   {:db db
    :dispatch [::add-game-log-entry (str "Creating new game with " player-count " players...")]
    :timeout {:id :game-creation
              :event [::game-created player-count]
              :time 1000}}))

(re-frame/reg-event-fx
 ::game-created
 (fn [{:keys [db]} [_ player-count]]
   (let [mock-game-state {:status :playing
                          :players (vec (for [i (range player-count)]
                                         {:id (str "Player-" (inc i))
                                          :status :playing
                                          :cash 1500
                                          :cell-residency 0}))
                          :current-turn {:player "Player-1" :dice-rolls []}
                          :transactions []}]
     {:db (assoc-in db [:single-game :state] mock-game-state)
      :dispatch-n [[::add-game-log-entry "Game created successfully!"]
                   [::add-game-log-entry "Players starting with $1500 each"]
                   [::start-game-loop]]})))

(re-frame/reg-event-fx
 ::start-game-loop
 (fn [{:keys [db]} _]
   {:dispatch [::add-game-log-entry "Game starting... Player-1's turn"]
    :timeout {:id :game-turn
              :event [::advance-game]
              :time 2000}}))

(re-frame/reg-event-fx
 ::advance-game
 (fn [{:keys [db]} _]
   (let [running? (get-in db [:single-game :running?])
         game-state (get-in db [:single-game :state])]
     (if (and running? game-state (= :playing (:status game-state)))
       (let [current-player (get-in game-state [:current-turn :player])
             dice-roll [(+ 1 (rand-int 6)) (+ 1 (rand-int 6))]
             roll-total (apply + dice-roll)
             new-transactions (conj (:transactions game-state)
                                   {:type :roll
                                    :player current-player
                                    :roll dice-roll
                                    :total roll-total})
             updated-state (assoc game-state :transactions new-transactions)]
         {:db (assoc-in db [:single-game :state] updated-state)
          :dispatch-n [[::add-game-log-entry 
                        (str current-player " rolled " dice-roll " (total: " roll-total ")")]
                       [::add-game-log-entry 
                        (str current-player " moved " roll-total " spaces")]]
          :timeout {:id :game-turn
                    :event [::advance-game] 
                    :time 3000}})
       {:dispatch [::add-game-log-entry "Game loop stopped"]}))))

(re-frame/reg-event-db
 ::add-game-log-entry
 (fn [db [_ entry]]
   (update-in db [:single-game :log] conj entry)))

;; Timeout effect
(re-frame/reg-fx
 :timeout
 (fn [{:keys [id event time]}]
   (js/setTimeout #(re-frame/dispatch event) time)))

;; Bulk simulation events
(re-frame/reg-event-db
 ::set-bulk-sim-running
 (fn [db [_ running?]]
   (assoc-in db [:bulk-simulation :running?] running?)))

(re-frame/reg-event-db
 ::set-bulk-sim-progress
 (fn [db [_ progress total]]
   (-> db
       (assoc-in [:bulk-simulation :progress] progress)
       (assoc-in [:bulk-simulation :total-games] total))))

(re-frame/reg-event-db
 ::set-bulk-sim-results
 (fn [db [_ results]]
   (assoc-in db [:bulk-simulation :results] results)))

(re-frame/reg-event-db
 ::set-bulk-sim-config
 (fn [db [_ config]]
   (assoc-in db [:bulk-simulation :config] config)))

;; UI state events
(re-frame/reg-event-db
 ::set-loading
 (fn [db [_ loading?]]
   (assoc db :loading? loading?)))

(re-frame/reg-event-db
 ::set-error
 (fn [db [_ error-message]]
   (assoc db :error-message error-message)))
