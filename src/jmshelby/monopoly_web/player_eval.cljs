(ns jmshelby.monopoly-web.player-eval
  (:require
   [sci.core :as sci]
   [clojure.set :as set]
   [jmshelby.monopoly.util :as util]
   [jmshelby.monopoly.core :as core]
   [jmshelby.monopoly.definitions :as defs]
   [jmshelby.monopoly.cards :as cards]
   [jmshelby.monopoly.player :as player]
   [jmshelby.monopoly.players.dumb :as dumb-player]))

;; Create a SCI context with the namespaces needed by player code
(defn create-sci-context []
  (sci/init {:namespaces {'clojure.set {'difference set/difference
                                        'union set/union
                                        'intersection set/intersection}
                          'jmshelby.monopoly.util {'player-by-id util/player-by-id
                                                   'owned-properties util/owned-properties
                                                   'owned-property-details util/owned-property-details
                                                   'street-group-counts util/street-group-counts
                                                   'potential-house-purchases util/potential-house-purchases
                                                   ;; Add more utility functions needed by player code
                                                   'half util/half
                                                   'rcompare util/rcompare
                                                   'sum util/sum
                                                   'other-players util/other-players
                                                   'current-player util/current-player
                                                   'has-bail-card? util/has-bail-card?}}
             :classes {'js goog/global :allow :all}}))

;; Evaluate player code and extract the decide function
(defn eval-player-code
  "Evaluates the player code string and returns the decide function.
   Returns nil if evaluation fails or decide function is not found."
  [code-str]
  (try
    (let [ctx (create-sci-context)
          ;; Evaluate the code in the SCI context
          _ (sci/eval-string* ctx code-str)
          ;; Try to get the decide function from the context
          decide-fn (sci/eval-string* ctx "decide")]
      (when (fn? decide-fn)
        decide-fn))
    (catch :default e
      (js/console.error "Error evaluating player code:" e)
      nil)))

;; Wrapper to make the evaluated decide function compatible with the game engine
(defn create-player-fn
  "Creates a player function from evaluated code that's compatible with the game engine."
  [code-str]
  (when-let [decide-fn (eval-player-code code-str)]
    (fn [game-state player-id method params]
      (decide-fn game-state player-id method params))))

;; Custom game initialization that supports custom player functions
(defn init-game-state-with-custom-players
  "Initialize a game state with custom player functions.
   player-configs: vector of maps with :id and :function keys
   If player-configs is a number, creates that many players with dumb-player logic."
  [player-configs]
  (let [;; Handle both number and player configs
        players (if (number? player-configs)
                  ;; If it's a number, create default players
                  (->> (range 65 (+ player-configs 65))
                       (map char)
                       (map str)
                       (map #(hash-map :id %
                                      :function dumb-player/decide
                                      :status :playing
                                      :cash 1500
                                      :cell-residency 0
                                      :cards #{}
                                      :properties {}))
                       vec)
                  ;; If it's player configs, use them
                  (->> player-configs
                       (map #(merge {:status :playing
                                    :cash 1500
                                    :cell-residency 0
                                    :cards #{}
                                    :properties {}}
                                   %))
                       vec))]
    ;; Define initial game state (copied from core/init-game-state)
    {:status       :playing
     :players      players
     :current-turn {:player     (-> players first :id)
                    :dice-rolls []}
     :board        defs/board
     :card-queue   (cards/cards->deck-queues (:cards defs/board))
     :transactions []
     :functions    {:move-to-cell           core/move-to-cell
                    :apply-dice-roll        core/apply-dice-roll
                    :make-requisite-payment player/make-requisite-payment}}))

;; Run a game to completion from initial state (similar to rand-game-end-state)
(defn- run-game-from-state
  "Run a game to completion from an initial state. Returns final game state."
  [initial-state safety-threshold]
  (letfn [(safe-advance [state iteration]
            (try
              (let [next-state (core/advance-board state)]
                {:state next-state :exception nil})
              (catch :default e
                {:state state
                 :exception {:message (.-message e)
                            :type (str (type e))
                            :iteration iteration
                            :last-transaction (last (:transactions state))
                            :current-player (get-in state [:current-turn :player])
                            :player-cash (->> state :players
                                             (map #(vector (:id %) (select-keys % [:cash :status])))
                                             (into {}))}})))]
    (loop [current-state initial-state
           iteration-count 0]
      (let [{:keys [state exception]} (safe-advance current-state iteration-count)]
        (cond
          ;; Exception occurred
          exception
          (assoc current-state :exception exception)

          ;; Game completed normally
          (= :completed (:status state))
          state

          ;; Failsafe - too many iterations
          (>= iteration-count safety-threshold)
          (assoc state :failsafe-stop true :iterations iteration-count)

          ;; Continue the game
          :else
          (recur state (inc iteration-count)))))))

;; Run a single game with custom player logic
(defn run-custom-game
  "Run a single game with custom player logic. Returns the final game state.
   custom-player-fn: the player decision function to use for all players
   num-players: number of players (default 4)
   safety-threshold: maximum iterations before stopping (default 2000)"
  ([custom-player-fn] (run-custom-game custom-player-fn 4))
  ([custom-player-fn num-players] (run-custom-game custom-player-fn num-players 2000))
  ([custom-player-fn num-players safety-threshold]
   (let [;; Create player configs with custom function
         player-configs (->> (range 65 (+ num-players 65))
                            (map char)
                            (map str)
                            (map #(hash-map :id % :function custom-player-fn))
                            vec)
         ;; Initialize game state
         initial-state (init-game-state-with-custom-players player-configs)
         ;; Run the game to completion
         final-state (run-game-from-state initial-state safety-threshold)]
     final-state)))
