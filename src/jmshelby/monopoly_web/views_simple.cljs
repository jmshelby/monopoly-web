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
     
     [:div {:style {:display "flex" :gap "2em" :margin-top "2em"}}
      [:div {:style {:flex "1"}}
       [:h3 "Game Summary"]
       [:div {:style {:background-color "#f5f5f5" :padding "1em" :min-height "200px"}}
        (if @game-state
          [:div
           [:p (str "Status: " (:status @game-state))]
           [:p (str "Total Transactions: " (count (:transactions @game-state)))]
           [:p (str "Active Players: " 
                   (->> (:players @game-state)
                        (filter #(= :playing (:status %)))
                        count))]
           [:p (str "Total Players: " (count (:players @game-state)))]
           (when (:exception @game-state)
             [:p {:style {:color "red"}} (str "Exception: " (get-in @game-state [:exception :message]))])
           (when (:failsafe-stop @game-state)
             [:p {:style {:color "orange"}} "Game ended due to failsafe limit"])
           (let [winner (->> (:players @game-state)
                            (filter #(= :playing (:status %)))
                            first)]
             (when (and winner (= 1 (->> (:players @game-state)
                                         (filter #(= :playing (:status %)))
                                         count)))
               [:p {:style {:color "green" :font-weight "bold"}} 
                (str "🏆 Winner: " (:id winner) " with $" (:cash winner))]))]
          [:p "No game loaded"])]]
      
      [:div {:style {:flex "1"}}
       [:h3 "Turn by Turn Narrative"]
       [:div {:style {:background-color "#f0f0f0" :padding "1em" :height "200px" :overflow "auto"}}
        (if (seq @game-log)
          (for [[idx entry] (map-indexed vector @game-log)]
            [:p {:key idx} entry])
          [:p "Game log will appear here..."])]]]
     
     [:div {:style {:margin-top "2em"}}
      [:h3 "Transaction Log"]
      [:div {:style {:background-color "#f8f8f8" :padding "1em" :height "300px" :overflow "auto"}}
       (if (and @game-state (:transactions @game-state) (seq (:transactions @game-state)))
         (for [[idx tx] (map-indexed vector (take-last 100 (:transactions @game-state)))]
           [:p {:key idx :style {:font-size "12px" :margin "2px 0"}} 
            (str (+ idx (- (count (:transactions @game-state)) 100) 1) ". " 
                (case (:type tx)
                  :roll (str "🎲 " (:player tx) " rolled " (:roll tx))
                  :purchase (str "🏠 " (:player tx) " bought " (:property tx) " for $" (:cost tx))
                  :rent (str "💰 " (:player tx) " paid $" (:amount tx) " rent to " (:to tx))
                  :go-allowance (str "✅ " (:player tx) " collected $" (:amount tx) " for passing GO")
                  :bankruptcy (str "💸 " (:player tx) " went bankrupt")
                  :tax (str "🏛️ " (:player tx) " paid $" (:amount tx) " tax")
                  :jail (str "🔒 " (:player tx) " went to jail")
                  :get-out-of-jail (str "🔓 " (:player tx) " got out of jail")
                  :purchase-house (str "🏘️ " (:player tx) " built house on " (:property tx))
                  :sell-house (str "🏗️ " (:player tx) " sold house from " (:property tx))
                  :mortgage (str "🏦 " (:player tx) " mortgaged " (:property tx))
                  :unmortgage (str "💳 " (:player tx) " unmortgaged " (:property tx))
                  :auction-initiated (str "⚖️ Auction started for " (:property tx))
                  :auction-completed (str "🔨 " (:winner tx) " won auction for " (:property tx) " ($" (:winning-bid tx) ")")
                  :card (str "🃏 " (:player tx) " drew card: " (:effect tx))
                  (str (:type tx) " - " (pr-str tx))))])
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