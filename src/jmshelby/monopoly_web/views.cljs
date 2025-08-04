(ns jmshelby.monopoly-web.views
  (:require
   [re-frame.core :as re-frame]
   [re-com.core :as re-com :refer [at]]
   [jmshelby.monopoly-web.styles :as styles]
   [jmshelby.monopoly-web.events :as events]
   [jmshelby.monopoly-web.routes :as routes]
   [jmshelby.monopoly-web.subs :as subs]))

;; Battle-opoly Landing Page
(defn battle-opoly-panel []
  [re-com/v-box
   :src      (at)
   :gap      "2em"
   :align    :center
   :justify  :center
   :height   "100vh"
   :children [[re-com/title
               :src   (at)
               :label "Monopolyistics"
               :level :level1
               :style {:font-size "3em" :margin-bottom "1em"}]
              
              [re-com/v-box
               :src      (at)
               :gap      "1em"
               :width    "300px"
               :children [[re-com/button
                           :src      (at)
                           :label    "Single Game w/Deep Analysis"
                           :class    "btn-primary"
                           :style    {:width "100%" :height "60px" :font-size "1.1em"}
                           :on-click #(try
                                       (re-frame/dispatch [::events/set-game-mode :single])
                                       (re-frame/dispatch [::events/navigate :setup])
                                       (catch js/Error e
                                         (js/console.log "Error in single game click:" e)))]
                          
                          [re-com/button
                           :src      (at)
                           :label    "Run lots of games w/stats"
                           :class    "btn-secondary"
                           :style    {:width "100%" :height "60px" :font-size "1.1em"}
                           :on-click #(try
                                       (re-frame/dispatch [::events/set-game-mode :bulk])
                                       (re-frame/dispatch [::events/navigate :setup])
                                       (catch js/Error e
                                         (js/console.log "Error in bulk game click:" e)))]]]]])

;; Setup Screen (Single or Bulk)
(defn player-config-row [idx player]
  (let [current-players (re-frame/subscribe [::subs/players])]
    [re-com/h-box
     :src      (at)
     :gap      "1em"
     :align    :center
     :children [[re-com/label
                 :src   (at)
                 :label (str "Player " (inc idx))]
                
                [re-com/single-dropdown
                 :src              (at)
                 :choices          [{:id :dumb-v1 :label "Dumb v1"}
                                    {:id :dumb-v2 :label "Dumb v2"} 
                                    {:id :future-player :label "Future Player v"}]
                 :model            (:type player :dumb-v1)
                 :width            "150px"
                 :on-change        #(re-frame/dispatch [::events/add-game-log-entry (str "Player " (inc idx) " type changed")])]]]))

(defn setup-panel []
  (let [mode (re-frame/subscribe [::subs/game-mode])
        players (re-frame/subscribe [::subs/players])]
    [re-com/v-box
     :src      (at)
     :gap      "2em"
     :padding  "2em"
     :children [[re-com/title
                 :src   (at)
                 :label (if (= @mode :single) "Single or Bulk?" "Setup Game")
                 :level :level1]
                
                [re-com/h-box
                 :src      (at)
                 :gap      "2em"
                 :children [[re-com/v-box
                             :src      (at)
                             :gap      "1em"
                             :children [[re-com/title
                                         :src   (at)
                                         :label "Players"
                                         :level :level3]
                                        
                                        [re-com/v-box
                                         :src      (at)
                                         :gap      "0.5em"
                                         :children (map-indexed player-config-row 
                                                               (or @players 
                                                                   [{:type :dumb-v1} {:type :dumb-v2} 
                                                                    {:type :dumb-v1} {:type :future-player}]))]
                                        
                                        [re-com/button
                                         :src      (at)
                                         :label    "+ Add Player"
                                         :style    {:margin-top "1em"}
                                         :on-click #(re-frame/dispatch [::events/update-players 
                                                                        (conj (or @players []) {:type :dumb-v1})])]]]
                            
                            (when (= @mode :bulk)
                              [re-com/v-box
                               :src      (at)
                               :gap      "1em"
                               :children [[re-com/title
                                           :src   (at)
                                           :label "Bulk Simulation Options"
                                           :level :level3]
                                          
                                          [re-com/h-box
                                           :src      (at)
                                           :gap      "1em"
                                           :align    :center
                                           :children [[re-com/label
                                                       :src   (at)
                                                       :label "Number of games:"]
                                                      [re-com/input-text
                                                       :src          (at)
                                                       :model        "1000"
                                                       :width        "100px"
                                                       :on-change    #()]]]]])]]
                
                [re-com/h-box
                 :src      (at)
                 :gap      "1em"
                 :children [[re-com/button
                             :src      (at)
                             :label    "← Back"
                             :on-click #(re-frame/dispatch [::events/navigate :battle-opoly])]
                            
                            [re-com/button
                             :src      (at)
                             :label    (if (= @mode :single) "Play!" "Run Simulation!")
                             :class    "btn-primary"
                             :on-click #(re-frame/dispatch [::events/navigate 
                                                            (if (= @mode :single) 
                                                              :single-game
                                                              :bulk-simulation)])]]]]]))

;; Single Game Interface
(defn game-summary-panel [game-state]
  [re-com/v-box
   :src      (at)
   :gap      "1em"
   :children [[re-com/title
               :src   (at)
               :label "Game Summary"
               :level :level3]
              
              [re-com/v-box
               :src      (at)
               :gap      "0.5em"
               :style    {:background-color "#f5f5f5" :padding "1em" :min-height "200px"}
               :children (if game-state
                           [[re-com/label :src (at) :label (str "Status: " (:status game-state))]
                            [re-com/label :src (at) :label (str "Turn: " (count (:transactions game-state)))]
                            [re-com/label :src (at) :label (str "Active Players: " 
                                                                 (->> (:players game-state)
                                                                      (filter #(= :playing (:status %)))
                                                                      count))]]
                           [[re-com/label :src (at) :label "No game loaded"]])]]])

(defn turn-narrative-panel []
  (let [game-log (re-frame/subscribe [::subs/game-log])]
    [re-com/v-box
     :src      (at)
     :gap      "1em"
     :children [[re-com/title
                 :src   (at)
                 :label "Turn by Turn Narrative"
                 :level :level3]
                
                [re-com/v-box
                 :src      (at)
                 :style    {:background-color "#f0f0f0" :padding "1em" :height "200px" :overflow "auto"}
                 :children (if (seq @game-log)
                             (map-indexed (fn [idx entry]
                                           [re-com/label
                                            :src (at)
                                            :key idx
                                            :label entry])
                                         @game-log)
                             [[re-com/label :src (at) :label "Game log will appear here..."]])]]]))

(defn transaction-log-panel []
  (let [game-state (re-frame/subscribe [::subs/single-game-state])]
    [re-com/v-box
     :src      (at)
     :gap      "1em"
     :children [[re-com/title
                 :src   (at)
                 :label "Transaction Log w/text"
                 :level :level3]
                
                [re-com/v-box
                 :src      (at)
                 :style    {:background-color "#f8f8f8" :padding "1em" :height "300px" :overflow "auto"}
                 :children (if (and @game-state (:transactions @game-state) (seq (:transactions @game-state)))
                             (map-indexed (fn [idx tx]
                                           [re-com/label
                                            :src (at)
                                            :key idx
                                            :label (str (inc idx) ". " (:type tx) " - " 
                                                       (case (:type tx)
                                                         :roll (str "Player " (:player tx) " rolled " (:roll tx))
                                                         (str tx)))])
                                         (:transactions @game-state))
                             [[re-com/label :src (at) :label "Transaction details will appear here..."]])]]]))

(defn single-game-panel []
  (let [game-state (re-frame/subscribe [::subs/single-game-state])
        running? (re-frame/subscribe [::subs/single-game-running?])]
    [re-com/v-box
     :src      (at)
     :padding  "2em"
     :gap      "2em"
     :children [[re-com/h-box
                 :src      (at)
                 :justify  :between
                 :children [[re-com/title
                             :src   (at)
                             :label "Single Game Play"
                             :level :level1]
                            
                            [re-com/h-box
                             :src      (at)
                             :gap      "1em"
                             :children [[re-com/button
                                         :src      (at)
                                         :label    "← Back to Setup"
                                         :on-click #(re-frame/dispatch [::events/navigate :setup])]
                                        
                                        [re-com/button
                                         :src      (at)
                                         :label    (if @running? "Stop Game" "Start Game")
                                         :class    (if @running? "btn-danger" "btn-success")
                                         :on-click #(re-frame/dispatch (if @running? 
                                                                        [::events/stop-single-game]
                                                                        [::events/start-single-game]))]
                                        
                                        (when @running?
                                          [re-com/button
                                           :src      (at)
                                           :label    "Roll Dice"
                                           :class    "btn-primary"
                                           :style    {:margin-left "10px"}
                                           :on-click #(re-frame/dispatch [::events/simulate-dice-roll])])]]]]
                
                [re-com/h-box
                 :src      (at)
                 :gap      "2em"
                 :children [[game-summary-panel @game-state]
                            [turn-narrative-panel]]]
                
                [transaction-log-panel]]]))

;; Bulk Simulation Interface  
(defn simulation-progress-panel []
  (let [running? (re-frame/subscribe [::subs/bulk-sim-running?])
        progress (re-frame/subscribe [::subs/bulk-sim-progress])
        total (re-frame/subscribe [::subs/bulk-sim-total])]
    [re-com/v-box
     :src      (at)
     :gap      "1em"
     :children [[re-com/title
                 :src   (at)
                 :label "Simulation Progress"
                 :level :level3]
                
                (if @running?
                  [re-com/progress-bar
                   :src        (at)
                   :model      (if (> @total 0) (* 100 (/ @progress @total)) 0)
                   :width      "100%"]
                  [re-com/label :src (at) :label "Ready to run simulation"])]]))

(defn simulation-results-panel []
  (let [results (re-frame/subscribe [::subs/bulk-sim-results])]
    [re-com/v-box
     :src      (at)
     :gap      "1em"
     :children [[re-com/title
                 :src   (at)
                 :label "Simulation Results"
                 :level :level3]
                
                [re-com/v-box
                 :src      (at)
                 :style    {:background-color "#f5f5f5" :padding "1em" :height "400px" :overflow "auto"}
                 :children (if @results
                             [[re-com/label :src (at) :label (str "Total Games: " (:total-games @results))]
                              [re-com/label :src (at) :label (str "Games with Winner: " (:games-with-winner @results))]
                              [re-com/label :src (at) :label (str "Average Transactions: " (get-in @results [:transaction-stats :avg]))]
                              [re-com/label :src (at) :label "Detailed statistics..."]]
                             [[re-com/label :src (at) :label "Run simulation to see results"]])]]]))

(defn bulk-simulation-panel []
  (let [running? (re-frame/subscribe [::subs/bulk-sim-running?])]
    [re-com/v-box
     :src      (at)
     :padding  "2em"
     :gap      "2em"
     :children [[re-com/h-box
                 :src      (at)
                 :justify  :between
                 :children [[re-com/title
                             :src   (at)
                             :label "Bulk Game Simulation"
                             :level :level1]
                            
                            [re-com/h-box
                             :src      (at)
                             :gap      "1em"
                             :children [[re-com/button
                                         :src      (at)
                                         :label    "← Back to Setup"
                                         :on-click #(re-frame/dispatch [::events/navigate :setup])]
                                        
                                        [re-com/button
                                         :src      (at)
                                         :label    (if @running? "Stop Simulation" "Start Simulation")
                                         :class    (if @running? "btn-danger" "btn-success")
                                         :on-click #(re-frame/dispatch [::events/set-bulk-sim-running (not @running?)])]]]]]
                
                [simulation-progress-panel]
                [simulation-results-panel]]]))

;; Panel routing
(defmethod routes/panels :battle-opoly-panel [] [battle-opoly-panel])
(defmethod routes/panels :setup-panel [] [setup-panel])
(defmethod routes/panels :single-game-panel [] [single-game-panel])
(defmethod routes/panels :bulk-simulation-panel [] [bulk-simulation-panel])

;; Main panel
(defn main-panel []
  (let [active-panel (re-frame/subscribe [::subs/active-panel])]
    [re-com/v-box
     :src      (at)
     :height   "100%"
     :children [(if @active-panel
                  (routes/panels @active-panel)
                  [re-com/label :src (at) :label "Loading..."])]]))
