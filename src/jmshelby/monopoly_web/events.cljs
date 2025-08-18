(ns jmshelby.monopoly-web.events
  (:require
   [clojure.core.async :as async]
   [re-frame.core :as re-frame]
   [jmshelby.monopoly-web.db :as db]
   [jmshelby.monopoly.util.time :as time]
   [jmshelby.monopoly.core :as monopoly-core]
   [jmshelby.monopoly.simulation :as core-sim]))

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

(re-frame/reg-event-db
 ::start-single-game
 (fn [db _]
   (let [players (get-in db [:game-setup :players])
         player-count (if (and players (seq players))
                        (count players)
                        4)
         initial-log [(str "Starting complete monopoly game with " player-count " players...")
                      "Running game simulation..."]]
     (-> db
         (assoc-in [:single-game :running?] true)
         (assoc-in [:single-game :log] initial-log)
         (assoc-in [:single-game :state] nil))

     ;; Run the actual monopoly game in a setTimeout to avoid blocking UI
     (js/setTimeout
       (fn []
         (try
           (let [final-game-state (monopoly-core/rand-game-end-state player-count)
                 winner-id (when (= 1 (->> (:players final-game-state)
                                           (filter #(= :playing (:status %)))
                                           count))
                             (->> (:players final-game-state)
                                  (filter #(= :playing (:status %)))
                                  first
                                  :id))
                 transaction-count (count (:transactions final-game-state))
                 completion-log [(str "Game completed! Total transactions: " transaction-count)
                                (if winner-id
                                  (str "Winner: " winner-id)
                                  "Game ended without a clear winner")
                                (if (:exception final-game-state)
                                  (str "Game ended with exception: " (get-in final-game-state [:exception :message]))
                                  "")
                                (if (:failsafe-stop final-game-state)
                                  "Game ended due to failsafe limit"
                                  "")]]
             (re-frame/dispatch [::set-single-game-state final-game-state])
             (re-frame/dispatch [::add-game-log-entries completion-log])
             (re-frame/dispatch [::set-single-game-running false]))
           (catch js/Error e
             (re-frame/dispatch [::add-game-log-entry (str "Error running game: " (.-message e))])
             (re-frame/dispatch [::set-single-game-running false]))))
       100)

     db)))

(re-frame/reg-event-db
 ::stop-single-game
 (fn [db _]
   (-> db
       (assoc-in [:single-game :running?] false)
       (update-in [:single-game :log] conj "Game stopped by user"))))

(re-frame/reg-event-db
 ::add-game-log-entry
 (fn [db [_ entry]]
   (update-in db [:single-game :log] conj entry)))

(re-frame/reg-event-db
 ::add-game-log-entries
 (fn [db [_ entries]]
   (update-in db [:single-game :log] #(apply conj % entries))))

(re-frame/reg-event-db
 ::set-single-game-running
 (fn [db [_ running?]]
   (assoc-in db [:single-game :running?] running?)))

(re-frame/reg-event-db
 ::simulate-dice-roll
 (fn [db _]
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
             updated-state (assoc game-state :transactions new-transactions)
             new-log-entries [(str current-player " rolled " dice-roll " (total: " roll-total ")")
                              (str current-player " moved " roll-total " spaces")]]
         (-> db
             (assoc-in [:single-game :state] updated-state)
             (update-in [:single-game :log] #(apply conj % new-log-entries))))
       db))))

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

(re-frame/reg-event-db
 ::set-bulk-sim-config-games
 (fn [db [_ games]]
   (assoc-in db [:bulk-simulation :config :games] games)))

(re-frame/reg-event-fx
 ::set-active-panel
 (fn [{:keys [db]} [_ active-panel]]
   {:db (assoc db :active-panel active-panel)}))

(re-frame/reg-event-fx
 ::start-bulk-simulation
 (fn [{:keys [db]} _]
   (let [num-games (get-in db [:bulk-simulation :config :games] 100)
         players (get-in db [:game-setup :players])
         player-count (if (and players (seq players))
                        (count players)
                        4)]

     {;; Reset the various status attributes
      :db (-> db
              (assoc-in [:bulk-simulation :start-time] (time/now))
              (assoc-in [:bulk-simulation :running?] true)
              (assoc-in [:bulk-simulation :progress] 0)
              (assoc-in [:bulk-simulation :total-games] num-games)
              (assoc-in [:bulk-simulation :results] []))
      ;; Start up a bulk monopoly simulation run
      :monopoly/simulation {:num-games num-games
                            :num-players player-count}})))

(re-frame/reg-event-fx
 ::stop-bulk-simulation
 (fn [{:keys [db]} _]
   {:db (assoc-in db [:bulk-simulation :running?] false)
    :monopoly/simulation-stop (get-in db [:bulk-simulation :output-chan])}))

(re-frame/reg-event-db
 ::bulk-sim-started
 (fn [db [_ output-ch]]
   (assoc-in db [:bulk-simulation :output-chan] output-ch)))

;; When bulk monopoly simulation is running, this gets fired every time
;; a new game is complete and ready to update the analysis screen
(re-frame/reg-event-fx
 ::bulk-sim-game-finished
 (fn [{:keys [db]} [_ game]]
   (println "registering new game results in db...")
   (let [start-time (get-in db [:bulk-simulation :start-time])
         duration-ms (time/elapsed-ms start-time
                                      (time/now))
         output-ch (get-in db [:bulk-simulation :output-chan])
         total-games (get-in db [:bulk-simulation :total-games])
         prev-results (get-in db [:bulk-simulation :results])
         new-results (conj prev-results game)
         new-stats (core-sim/calculate-statistics new-results
                                                  (count new-results)
                                                  duration-ms)
         more-games? (not= total-games (count new-results))]

     (merge
      {:db (-> db
              ;; Recalc new bulk stats with this additional game
               (assoc-in [:bulk-simulation :stats] new-stats)
              ;; Keep that game's results
               (assoc-in [:bulk-simulation :results] new-results)
              ;; Update progress counter
               (assoc-in [:bulk-simulation :progress] (count new-results))
               (assoc-in [:bulk-simulation :running?] more-games?))}
      ;; If there are more games needed, we need to invoke an fx for that
      (when more-games?
        [:monopoly/simulation-continue output-ch])))))


(re-frame/reg-event-db
 ::set-bulk-sim-progress
 (fn [db [_ progress]]
   (assoc-in db [:bulk-simulation :progress] progress)))

;; UI state events
(re-frame/reg-event-db
 ::set-loading
 (fn [db [_ loading?]]
   (assoc db :loading? loading?)))

(re-frame/reg-event-db
 ::set-error
 (fn [db [_ error-message]]
   (assoc db :error-message error-message)))
