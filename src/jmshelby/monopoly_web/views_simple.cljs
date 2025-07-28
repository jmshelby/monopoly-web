(ns jmshelby.monopoly-web.views-simple
  (:require
   [re-frame.core :as re-frame]
   [jmshelby.monopoly-web.events :as events]
   [jmshelby.monopoly-web.routes :as routes]
   [jmshelby.monopoly-web.subs :as subs]))

;; Simple HTML-based components without re-com
(defn simple-battle-opoly-panel []
  [:div {:style {:text-align "center" :padding "2em"}}
   [:h1 "Battle-opoly"]
   [:div
    [:button {:style {:display "block" :margin "1em auto" :padding "1em 2em"}
              :on-click #(do
                          (re-frame/dispatch [::events/set-game-mode :single])
                          (re-frame/dispatch [::events/navigate :setup]))}
     "Single Game w/Deep Analysis"]
    [:button {:style {:display "block" :margin "1em auto" :padding "1em 2em"}
              :on-click #(do
                          (re-frame/dispatch [::events/set-game-mode :bulk])
                          (re-frame/dispatch [::events/navigate :setup]))}
     "Run lots of games w/stats"]]])

(defn simple-setup-panel []
  (let [mode (re-frame/subscribe [::subs/game-mode])]
    [:div {:style {:padding "2em"}}
     [:h1 "Setup Game"]
     [:div
      [:h3 "Players"]
      [:p "Player 1: Dumb v1"]
      [:p "Player 2: Dumb v2"] 
      [:p "Player 3: Dumb v1"]
      [:p "Player 4: Future Player v"]]
     [:div
      [:button {:style {:margin-right "1em"}
                :on-click #(re-frame/dispatch [::events/navigate :battle-opoly])}
       "← Back"]
      [:button {:style {:background-color "#007bff" :color "white" :border "none" :padding "0.5em 1em"}
                :on-click #(re-frame/dispatch [::events/navigate 
                                              (if (= @mode :single) 
                                                :single-game
                                                :bulk-simulation)])}
       (if (= @mode :single) "Play!" "Run Simulation!")]]]))

(defn simple-single-game-panel []
  (let [game-state (re-frame/subscribe [::subs/single-game-state])
        running? (re-frame/subscribe [::subs/single-game-running?])
        game-log (re-frame/subscribe [::subs/game-log])]
    [:div {:style {:padding "2em"}}
     [:h1 "Single Game Play"]
     [:div
      [:button {:style {:margin-right "1em"}
                :on-click #(re-frame/dispatch [::events/navigate :setup])}
       "← Back to Setup"]
      [:button {:style {:background-color (if @running? "#dc3545" "#28a745")
                        :color "white" :border "none" :padding "0.5em 1em"}
                :on-click #(re-frame/dispatch (if @running? 
                                              [::events/stop-single-game]
                                              [::events/start-single-game]))}
       (if @running? "Stop Game" "Start Game")]
      (when @running?
        [:button {:style {:background-color "#007bff" :color "white" :border "none" 
                          :padding "0.5em 1em" :margin-left "1em"}
                  :on-click #(re-frame/dispatch [::events/simulate-dice-roll])}
         "Roll Dice"])]
     
     [:div {:style {:margin-top "2em"}}
      [:h3 "Game Summary"]
      [:div {:style {:background-color "#f5f5f5" :padding "1em" :min-height "200px" :font-family "monospace" :font-size "12px" :overflow "auto"}}
       (if @game-state
         (let [players (:players @game-state)
               transactions (:transactions @game-state)
               active-players (->> players (filter #(= :playing (:status %))))
               bankrupt-players (->> players (filter #(= :bankrupt (:status %))))
               winner (when (= 1 (count active-players)) (first active-players))
               tx-by-type (frequencies (map :type transactions))
               total-cash (apply + (map :cash players))
               total-properties (apply + (map #(count (:properties %)) players))
               
               ;; Calculate bank flows
               money-to-bank (->> transactions
                                  (filter #(= (:to %) :bank))
                                  (map :amount)
                                  (apply + 0))
               money-from-bank (->> transactions
                                    (filter #(= (:from %) :bank))
                                    (map :amount)
                                    (apply + 0))
               net-bank-flow (- money-to-bank money-from-bank)
               
               ;; Auction stats
               auction-initiated (->> transactions (filter #(= :auction-initiated (:type %))) count)
               auction-completed (->> transactions (filter #(= :auction-completed (:type %))) count)
               auction-passed (->> transactions (filter #(= :auction-passed (:type %))) count)]
           
           [:div
            ;; Game Overview
            [:div {:style {:margin-bottom "1em"}}
             [:strong "📊 GAME OVERVIEW"] [:br]
             (str "   Status: " (name (:status @game-state))) [:br]
             (when winner
               (str "   🏆 Winner: Player " (:id winner))) [:br]
             (str "   Total Transactions: " (count transactions)) [:br]]
            
            ;; Players
            [:div {:style {:margin-bottom "1em"}}
             [:strong "👥 PLAYERS"] [:br]
             (str "   Total: " (count players) " (Active: " (count active-players) 
                  ", Bankrupt: " (count bankrupt-players) ")") [:br]]
            
            ;; Economics
            [:div {:style {:margin-bottom "1em"}}
             [:strong "💰 ECONOMICS"] [:br]
             (str "   Total Cash in Circulation: $" total-cash) [:br]
             (str "   Properties Owned: " total-properties) [:br]
             (str "   Money Paid to Bank: $" money-to-bank) [:br]
             (str "   Money Received from Bank: $" money-from-bank) [:br]
             (str "   Net Bank Flow: $" (Math/abs net-bank-flow) 
                  (if (pos? net-bank-flow) " (to bank)" " (from bank)")) [:br]]
            
            ;; Transaction Breakdown (top 5)
            [:div {:style {:margin-bottom "1em"}}
             [:strong "📝 TRANSACTION BREAKDOWN"] [:br]
             (for [[tx-type count] (take 5 (sort-by second > tx-by-type))]
               [:div {:key tx-type} (str "   " (name tx-type) ": " count) [:br]])]
            
            ;; Player Outcomes
            [:div {:style {:margin-bottom "1em"}}
             [:strong "🎯 PLAYER OUTCOMES"] [:br]
             (for [player players]
               [:div {:key (:id player)} 
                (str "   Player " (:id player) " (" (name (:status player)) "): $" 
                     (:cash player) " cash, " (count (:properties player)) " properties") [:br]])]
            
            ;; Auctions (if any)
            (when (> auction-initiated 0)
              [:div {:style {:margin-bottom "1em"}}
               [:strong "🏛️ AUCTION ANALYSIS"] [:br]
               (str "   Total Auctions Initiated: " auction-initiated) [:br]
               (str "   Auctions Completed: " auction-completed 
                    " (" (if (> auction-initiated 0) 
                           (.toFixed (* 100 (/ auction-completed auction-initiated)) 1) 
                           "0") "%)") [:br]
               (str "   Auctions Passed: " auction-passed 
                    " (" (if (> auction-initiated 0) 
                           (.toFixed (* 100 (/ auction-passed auction-initiated)) 1) 
                           "0") "%)") [:br]])
            
            ;; Bankruptcies (if any)
            (when (seq bankrupt-players)
              [:div {:style {:margin-bottom "1em"}}
               [:strong "💸 BANKRUPTCIES"] [:br]
               (for [player bankrupt-players]
                 [:div {:key (:id player)} 
                  (str "   Player " (:id player) " went bankrupt") [:br]])])
            
            ;; Game Health
            [:div
             (if (and (:exception @game-state) (:failsafe-stop @game-state))
               [:span {:style {:color "red"}} "❌ Game ended with issues"]
               [:span {:style {:color "green"}} "✅ Game completed normally"])]])
         [:p "No game loaded"])]]
     
     [:div {:style {:margin-top "2em"}}
      [:h3 "Transaction Log"]
      [:div {:style {:background-color "#f8f8f8" :padding "1em" :height "600px" :overflow "auto" :font-family "monospace"}}
       (if (and @game-state (:transactions @game-state) (seq (:transactions @game-state)))
         (for [[idx tx] (map-indexed vector (:transactions @game-state))]
           [:p {:key idx :style {:font-size "11px" :margin "1px 0" :line-height "1.3"}} 
            (let [tx-num (inc idx)
                  format-money (fn [amount] (str "$" amount))
                  format-property (fn [prop-name] 
                                   (if prop-name
                                     (clojure.string/replace (name prop-name) #"-" " ")
                                     "unknown"))]
              (case (:type tx)
                :roll 
                (let [roll-result (:roll tx)
                      roll-total (apply + roll-result)
                      is-double? (apply = roll-result)]
                  (str "[" tx-num "] " (:player tx) " rolls " roll-total
                       (when is-double? " (rolled double)")))
                
                :move
                (str "[" tx-num "] " (:player tx) " moves to " 
                     (get {:go "GO" :jail "Jail" :free "Free Parking" :go-to-jail "Go to Jail"} 
                          (:after-cell tx) (str "Cell " (:after-cell tx))))
                
                :purchase 
                (str "[" tx-num "] " (:player tx) " purchases " 
                     (format-property (:property tx)) " for " (format-money (:price tx)))
                
                :payment
                (cond 
                  (= (:reason tx) :rent)
                  (str "[" tx-num "] *" (:from tx) " pays " (format-money (:amount tx)) 
                       " rent to " (:to tx) "*")
                  
                  (= (:reason tx) :tax)
                  (str "[" tx-num "] " (:from tx) " pays " (format-money (:amount tx)) " tax to bank")
                  
                  (= (:reason tx) :allowance)
                  (str "[" tx-num "] " (:to tx) " collects " (format-money (:amount tx)) " passing GO")
                  
                  :else
                  (str "[" tx-num "] " (:from tx) " pays " (format-money (:amount tx)) 
                       " to " (:to tx) " (" (name (:reason tx)) ")"))
                
                :purchase-house 
                (str "[" tx-num "] " (:player tx) " builds house on " 
                     (format-property (:property tx)) " for " (format-money (:price tx)))
                
                :sell-house 
                (str "[" tx-num "] " (:player tx) " sells house on " 
                     (format-property (:property tx)) " for " (format-money (:proceeds tx)))
                
                :mortgage-property 
                (str "[" tx-num "] " (:player tx) " mortgages " 
                     (format-property (:property tx)) " for " (format-money (:proceeds tx)))
                
                :unmortgage-property 
                (str "[" tx-num "] " (:player tx) " unmortgages " 
                     (format-property (:property tx)) " for " (format-money (:cost tx)))
                
                :bail 
                (case (first (:means tx))
                  :roll (str "[" tx-num "] " (:player tx) " gets out of jail with double roll " (second (:means tx)))
                  :cash (str "[" tx-num "] " (:player tx) " pays " (format-money (second (:means tx))) " bail to get out of jail")
                  :card (str "[" tx-num "] " (:player tx) " uses Get Out of Jail Free card")
                  (str "[" tx-num "] " (:player tx) " gets out of jail"))
                
                :go-to-jail 
                (str "[" tx-num "] " (:player tx) " goes to jail")
                
                :bankruptcy 
                (str "[" tx-num "] !!!" (:player tx) " goes bankrupt!!!")
                
                :auction-initiated 
                (str "[" tx-num "] Auction initiated for " (format-property (:property tx)))
                
                :auction-completed 
                (str "[" tx-num "] " (:winner tx) " wins auction for " 
                     (format-property (:property tx)) " with bid " (format-money (:winning-bid tx)))
                
                :trade 
                (if (= (:status tx) :accept)
                  (str "[" tx-num "] " (:from tx) " and " (:to tx) " complete trade")
                  (str "[" tx-num "] " (:from tx) " proposes trade to " (:to tx)))
                
                :card 
                (str "[" tx-num "] " (:player tx) " draws card: " (:description tx))
                
                ;; Default case
                (str "[" tx-num "] " (:type tx) " - " (pr-str (dissoc tx :type)))))])
         [:p "Transaction details will appear here..."])]]]))

(defn simple-bulk-simulation-panel []
  [:div {:style {:padding "2em"}}
   [:h1 "Bulk Game Simulation"]
   [:button {:style {:margin-right "1em"}
             :on-click #(re-frame/dispatch [::events/navigate :setup])}
    "← Back to Setup"]
   [:p "Bulk simulation interface coming soon..."]])

;; Panel routing for simple components
(defmethod routes/panels :battle-opoly-panel [] [simple-battle-opoly-panel])
(defmethod routes/panels :setup-panel [] [simple-setup-panel])
(defmethod routes/panels :single-game-panel [] [simple-single-game-panel])
(defmethod routes/panels :bulk-simulation-panel [] [simple-bulk-simulation-panel])

;; Simple main panel
(defn simple-main-panel []
  (let [active-panel (re-frame/subscribe [::subs/active-panel])]
    [:div {:style {:height "100vh"}}
     (if @active-panel
       (routes/panels @active-panel)
       [:div "Loading..."])]))