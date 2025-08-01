(ns jmshelby.monopoly-web.events
  (:require
   [re-frame.core :as re-frame]
   [jmshelby.monopoly-web.db :as db]
   [jmshelby.monopoly.core :as monopoly-core]
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

(re-frame/reg-event-db
 ::start-bulk-simulation
 (fn [db _]
   (let [num-games (get-in db [:bulk-simulation :config :games] 100)
         players (get-in db [:game-setup :players])
         player-count (if (and players (seq players)) 
                        (count players) 
                        4)]
     (-> db
         (assoc-in [:bulk-simulation :running?] true)
         (assoc-in [:bulk-simulation :progress] 0)
         (assoc-in [:bulk-simulation :total-games] num-games)
         (assoc-in [:bulk-simulation :results] nil))
     
     ;; Run bulk simulation in background
     (js/setTimeout 
       (fn []
         (try
           (let [start-time (js/Date.now)
                 results (atom [])]
             (letfn [(run-games [remaining]
                       (when (> remaining 0)
                         (let [game-result (monopoly-core/rand-game-end-state player-count)
                               active-players (->> (:players game-result)
                                                   (filter #(= :playing (:status %))))
                               game-completed? (= :complete (:status game-result))
                               winner-id (when (and game-completed? (= 1 (count active-players)))
                                           (:id (first active-players)))
                               ;; Debug logging
                               _ (js/console.log "Game result status:" (:status game-result))
                               _ (js/console.log "Active players count:" (count active-players))
                               _ (js/console.log "Game completed?" game-completed?)
                               _ (js/console.log "Has failsafe?" (:failsafe-stop game-result))
                               _ (js/console.log "Has exception?" (:exception game-result))
                               _ (js/console.log "Winner ID:" winner-id)
                               game-summary {:has-winner (some? winner-id)
                                             :winner-id winner-id
                                             :transaction-count (count (:transactions game-result))
                                             :exception? (boolean (:exception game-result))
                                             :failsafe? (boolean (:failsafe-stop game-result))}]
                           
                           (swap! results conj game-summary)
                           (re-frame/dispatch [::set-bulk-sim-progress (- num-games remaining -1)])
                           
                           ;; Continue with next game (small delay to prevent UI blocking)
                           (js/setTimeout #(run-games (dec remaining)) 10))))]
             
               ;; Start running games
               (run-games num-games))
             
             ;; When all games complete, calculate final statistics
             (js/setTimeout 
               (fn []
                 (let [end-time (js/Date.now)
                       duration-ms (- end-time start-time)
                       duration-sec (/ duration-ms 1000.0)
                       games-with-winner (->> @results (filter :has-winner))
                       games-without-winner (->> @results (filter #(not (:has-winner %))))
                       failsafe-games (->> @results (filter :failsafe?))
                       winner-counts (->> games-with-winner
                                          (group-by :winner-id)
                                          (map (fn [[id games]] [id (count games)]))
                                          (sort-by second >))
                       tx-counts (->> @results (map :transaction-count))
                       
                       final-results {:total-games num-games
                                      :duration-seconds duration-sec
                                      :games-per-second (/ num-games duration-sec)
                                      :games-with-winner (count games-with-winner)
                                      :games-without-winner (count games-without-winner)
                                      :failsafe-stops (count failsafe-games)
                                      :winner-percentage (* 100.0 (/ (count games-with-winner) num-games))
                                      :failsafe-percentage (* 100.0 (/ (count failsafe-games) num-games))
                                      :winner-distribution winner-counts
                                      :transaction-stats (when (seq tx-counts)
                                                           {:min (apply min tx-counts)
                                                            :max (apply max tx-counts)
                                                            :avg (/ (apply + tx-counts) (count tx-counts))})
                                      :games-with-auctions 0  ;; TODO: implement auction tracking
                                      :auction-occurrence-rate 0.0
                                      :total-auctions-initiated 0
                                      :auction-completion-rate 0.0}]
                   
                   (re-frame/dispatch [::set-bulk-sim-results final-results])
                   (re-frame/dispatch [::set-bulk-sim-running false])))
               500))
           (catch js/Error e
             (re-frame/dispatch [::set-bulk-sim-running false])
             (js/console.error "Error in bulk simulation:" e))))
       100)
     
     db)))

(re-frame/reg-event-db
 ::stop-bulk-simulation
 (fn [db _]
   (assoc-in db [:bulk-simulation :running?] false)))

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
