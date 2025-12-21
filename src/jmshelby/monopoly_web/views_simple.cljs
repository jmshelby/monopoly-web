(ns jmshelby.monopoly-web.views-simple
  (:require
   [cljs.pprint :refer [pprint]]
   [re-frame.core :as re-frame]
   [reagent.core :as r]
   [jmshelby.monopoly.simulation.output :as output]
   [jmshelby.monopoly-web.events :as events]
   [jmshelby.monopoly-web.routes :as routes]
   [jmshelby.monopoly-web.subs :as subs]
   [jmshelby.monopoly.analysis :as analysis]
   [reagent.dom :as rdom]
   ["@codemirror/state" :refer [EditorState]]
   ["@codemirror/view" :refer [EditorView lineNumbers keymap highlightActiveLine]]
   ["@codemirror/commands" :refer [defaultKeymap history historyKeymap]]
   ["@codemirror/language" :refer [bracketMatching]]
   ["@codemirror/theme-one-dark" :refer [oneDark]]
   [nextjournal.clojure-mode :as cm]
   [applied-science.js-interop :as j]
   [jmshelby.monopoly-web.player-templates :as templates]))

;; CodeMirror 6 editor component using nextjournal pattern
(defn clojure-editor [props]
  (r/with-let [!view (or (:view-atom props) (r/atom nil))
               mount! (fn [el]
                        (when el
                          (let [initial-value (or (:value props) "")
                                extensions #js [oneDark
                                                (history)
                                                (lineNumbers)
                                                (highlightActiveLine)
                                                (bracketMatching)
                                                cm/default-extensions
                                                (.of keymap cm/complete-keymap)
                                                (.of keymap historyKeymap)]
                                state (.create EditorState
                                               #js {:doc initial-value
                                                    :extensions extensions})
                                view (new EditorView #js {:state state :parent el})]
                            (reset! !view view))))]
    [:div.player-lab-textarea {:ref mount!}]

    (finally
      (when @!view
        (.destroy @!view)))))

;; Simple HTML-based components without re-com
(defn simple-battle-opoly-panel []
  [:div {:style {:text-align "center" :padding "2em"}}
   [:h1 "Monopolyistics"]
   [:div
    [:button {:class "btn-primary"
              :style {:display "block" :margin "1em auto"}
              :on-click #(do
                          (re-frame/dispatch [::events/set-game-mode :single])
                          (re-frame/dispatch [::events/navigate :setup]))}
     "Single Game w/Deep Analysis"]
    [:button {:class "btn-primary"
              :style {:display "block" :margin "1em auto"}
              :on-click #(do
                          (re-frame/dispatch [::events/set-game-mode :bulk])
                          (re-frame/dispatch [::events/navigate :setup]))}
     "Run lots of games w/stats"]
    [:button {:class "btn-info"
              :style {:display "block" :margin "1em auto"}
              :on-click #(re-frame/dispatch [::events/navigate :player-lab])}
     "Player Lab"]]])

(defn simple-setup-panel []
  (let [mode (re-frame/subscribe [::subs/game-mode])]
    [:div {:style {:padding "2em"}}
     [:h1 "Setup Game"]
     [:div
      [:h3 "Players"]
      [:p "Player 1: Dumb v1"]
      [:p "Player 2: Dumb v1"]
      [:p "Player 3: Dumb v1"]
      [:p "Player 4: Dumb v1"]]
     [:div
      [:button {:class "btn-secondary"
                :style {:margin-right "1em"}
                :on-click #(re-frame/dispatch [::events/navigate :battle-opoly])}
       "← Back"]
      [:button {:class "btn-success"
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
       (if @running? "Stop Game" "Play Game")]
      (when @running?
        [:button {:style {:background-color "#007bff" :color "white" :border "none"
                          :padding "0.5em 1em" :margin-left "1em"}
                  :on-click #(re-frame/dispatch [::events/simulate-dice-roll])}
         "Roll Dice"])]

     [:div {:style {:margin-top "2em"}}
      [:h3 "Game Summary"]
      [:div {:class "code-block" :style {:min-height "200px" :font-size "12px"}}
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
      [:div {:class "code-block" :style {:height "600px"}}
       (if (and @game-state (:transactions @game-state) (seq (:transactions @game-state)))
         [:pre {:style {:font-size "11px" :margin "0" :line-height "1.3" :white-space "pre-wrap"
                        :color "#cccccc" :background-color "transparent"}}
          (with-out-str (analysis/print-transaction-log @game-state))]
         [:p "Transaction details will appear here..."])]]]))

(defn simple-bulk-simulation-panel []
  (let [running? (re-frame/subscribe [::subs/bulk-sim-running?])
        progress (re-frame/subscribe [::subs/bulk-sim-progress])
        total-games (re-frame/subscribe [::subs/bulk-sim-total])
        results (re-frame/subscribe [::subs/bulk-sim-results])
        stats (re-frame/subscribe [::subs/bulk-sim-stats])
        num-games (re-frame/subscribe [::subs/bulk-sim-config-games])]
    [:div {:style {:padding "2em"}}
     [:h1 "Bulk Game Simulation"]
     [:div
      [:button {:style {:margin-right "1em"}
                :on-click #(re-frame/dispatch [::events/navigate :setup])}
       "← Back to Setup"]

      (when-not @running?
        [:button {:style {:background-color "#28a745" :color "white" :border "none"
                          :padding "0.5em 1em" :margin-left "1em"}
                  :on-click #(re-frame/dispatch [::events/start-bulk-simulation])}
         "Start Simulation"])

      (when @running?
        [:button {:style {:background-color "#dc3545" :color "white" :border "none"
                          :padding "0.5em 1em" :margin-left "1em"}
                  :on-click #(re-frame/dispatch [::events/stop-bulk-simulation])}
         "Stop Simulation"])]

     ;; Configuration section
     (when-not @running?
       [:div {:class "code-block" :style {:margin-top "2em"}}
        [:h3 "Simulation Configuration"]
        [:div
         [:label "Number of games: "]
         [:select {:value (or @num-games 100)
                   :on-change #(re-frame/dispatch [::events/set-bulk-sim-config-games
                                                   (js/parseInt (.. % -target -value))])}
          [:option {:value 50} "50"]
          [:option {:value 100} "100"]
          [:option {:value 500} "500"]
          [:option {:value 1000} "1000"]
          [:option {:value 2000} "2000"]]]])

     ;; Progress section
     (when @running?
       [:div {:style {:margin-top "2em"}}
        [:h3 "Simulation Progress"]
        [:div {:style {:background-color "#e9ecef" :height "20px" :border-radius "10px" :overflow "hidden"}}
         [:div {:style {:background-color "#007bff"
                        :height "100%"
                        :width (str (if (and @progress @total-games (> @total-games 0))
                                      (* 100 (/ @progress @total-games))
                                      0) "%")
                        :transition "width 0.3s ease"}}]]
        [:p (str "Completed " (or @progress 0) " / " (or @total-games 0) " games")]])

     ;; Results section
     (when @stats
       [:div {:style {:margin-top "2em"}}
        [:h3 "Simulation Results"]
        [:div {:class "code-block" :style {:font-size "12px"}}

         (with-out-str
           (output/print-simulation-results @stats))

         ;
         ]])]))

(defn simple-player-lab-panel []
  (let [running? (re-frame/subscribe [::subs/player-lab-running?])
        progress (re-frame/subscribe [::subs/player-lab-progress])
        total-games (re-frame/subscribe [::subs/player-lab-total])
        stats (re-frame/subscribe [::subs/player-lab-stats])]
    (r/with-let [!editor-view (r/atom nil)
                 get-editor-content (fn []
                                     (when-let [view @!editor-view]
                                       (-> view .-state .-doc .toString)))
                 run-simulation (fn []
                                  (when-let [code (get-editor-content)]
                                    (re-frame/dispatch [::events/run-player-lab-simulation code])))]
      [:div.player-lab-container
       ;; Left Panel: Code Editor
       [:div.player-lab-left-panel
        [:div.player-lab-header
         [:h3 "Player Logic Editor"]
         [:div.player-lab-buttons
          [:button.btn-secondary
           {:on-click #(re-frame/dispatch [::events/navigate :battle-opoly])}
           "← Back"]
          [:button.btn-success
           {:on-click run-simulation
            :disabled @running?}
           (if @running? "Running..." "Run Simulation")]]]

        ;; Code Editor Area
        [:div.player-lab-editor-area
         [:h4 "Code:"]
         [clojure-editor {:value templates/dumb-player-template
                         :view-atom !editor-view}]]]

       ;; Right Panel: Stats/Results
       [:div.player-lab-right-panel
        [:h3 "Simulation Results"]

        ;; Progress section
        (when @running?
          [:div {:style {:margin-bottom "2em"}}
           [:h4 "Progress"]
           [:div {:style {:background-color "#e9ecef" :height "20px" :border-radius "10px" :overflow "hidden"}}
            [:div {:style {:background-color "#007bff"
                           :height "100%"
                           :width (str (if (and @progress @total-games (> @total-games 0))
                                         (* 100 (/ @progress @total-games))
                                         0) "%")
                           :transition "width 0.3s ease"}}]]
           [:p (str "Completed " (or @progress 0) " / " (or @total-games 0) " games")]])

        ;; Results section
        (if @stats
          [:div.player-lab-results
           ;; Custom Player Performance (if available)
           (when (:custom-player-games @stats)
             [:div {:style {:margin-bottom "2em"
                           :padding "1em"
                           :background-color "#f8f9fa"
                           :border-left "4px solid #28a745"
                           :border-radius "4px"}}
              [:h4 {:style {:margin-top "0" :color "#28a745"}} "🎯 Custom Player Performance"]
              [:div {:style {:font-family "monospace" :font-size "14px"}}
               [:p [:strong "Win Rate: "]
                (str (:custom-player-wins @stats) " / " (:custom-player-games @stats)
                     " (" (.toFixed (:custom-player-win-percentage @stats) 1) "%)")]
               [:p [:strong "Expected (baseline): "] "25.0% (1 in 4 players)"]
               [:p [:strong "Performance Ratio: "]
                [:span {:style {:color (if (>= (:custom-player-performance-ratio @stats) 1.0)
                                        "#28a745"  ; green if better
                                        "#dc3545")  ; red if worse
                               :font-weight "bold"}}
                 (str (.toFixed (:custom-player-performance-ratio @stats) 2) "x")]
                " "
                (cond
                  (>= (:custom-player-performance-ratio @stats) 1.2) "🚀 Significantly better!"
                  (>= (:custom-player-performance-ratio @stats) 1.0) "✓ Better than baseline"
                  (>= (:custom-player-performance-ratio @stats) 0.8) "≈ Similar to baseline"
                  :else "⚠ Worse than baseline")]]])

           ;; General Stats
           [:div {:class "code-block" :style {:font-size "12px"}}
            (with-out-str
              (output/print-simulation-results @stats))]]
          [:div.player-lab-placeholder
           [:p "Simulation results will appear here after running the code."]])]])))

;; Panel routing for simple components
(defmethod routes/panels :battle-opoly-panel [] [simple-battle-opoly-panel])
(defmethod routes/panels :setup-panel [] [simple-setup-panel])
(defmethod routes/panels :single-game-panel [] [simple-single-game-panel])
(defmethod routes/panels :bulk-simulation-panel [] [simple-bulk-simulation-panel])
(defmethod routes/panels :player-lab-panel [] [simple-player-lab-panel])

;; Simple main panel
(defn simple-main-panel []
  (let [active-panel (re-frame/subscribe [::subs/active-panel])]
    [:div {:style {:height "100vh"}}
     (if @active-panel
       (routes/panels @active-panel)
       [:div "Loading..."])]))
