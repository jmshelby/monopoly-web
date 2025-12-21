(ns jmshelby.monopoly-web.player-eval
  (:require
   [sci.core :as sci]
   [clojure.set :as set]
   [clojure.string]
   [jmshelby.monopoly.util :as util]
   [jmshelby.monopoly.core :as core]
   [jmshelby.monopoly.definitions :as defs]
   [jmshelby.monopoly.cards :as cards]
   [jmshelby.monopoly.player :as player]
   [jmshelby.monopoly.players.dumb :as dumb-player]))

;; Create a SCI context with the namespaces needed by player code
(defn create-sci-context []
  (let [;; Define the utility functions map once
        util-fns {'player-by-id util/player-by-id
                  'owned-properties util/owned-properties
                  'owned-property-details util/owned-property-details
                  'street-group-counts util/street-group-counts
                  'potential-house-purchases util/potential-house-purchases
                  'half util/half
                  'rcompare util/rcompare
                  'sum util/sum
                  'other-players util/other-players
                  'current-player util/current-player
                  'has-bail-card? util/has-bail-card?}
        ;; Define the set functions map once
        set-fns {'difference set/difference
                 'union set/union
                 'intersection set/intersection}]
    (sci/init {:namespaces {'clojure.set set-fns
                            'set set-fns  ;; Add alias
                            'jmshelby.monopoly.util util-fns
                            'util util-fns}  ;; Add alias
               :classes {'js goog/global :allow :all}
               :bindings {'*ns* (sci/create-ns 'user nil)}})))

(defn- strip-ns-form
  "Remove the ns form from the code string since we provide namespaces via SCI context.
   This is a simple heuristic that removes everything from (ns to the first line that starts with (def"
  [code-str]
  (let [;; Find the first (defn, (defn-, (def, etc after the ns form
        first-def-idx (or (clojure.string/index-of code-str "\n(def")
                          ;; If no newline before def, try without newline
                          (clojure.string/index-of code-str "(def"))
        ;; If we found a def, take everything from there onwards
        result (if first-def-idx
                 (subs code-str first-def-idx)
                 ;; Otherwise just try removing the ns form with a simple regex
                 (clojure.string/replace code-str #"(?s)^\s*\(ns\s+.*?\n\n" ""))]
    (clojure.string/trim result)))

;; Evaluate player code and extract the decide function
(defn eval-player-code
  "Evaluates the player code string and returns the decide function.
   Returns nil if evaluation fails or decide function is not found."
  [code-str]
  (try
    (let [ctx (create-sci-context)
          ;; Strip ns form since we provide namespaces manually
          code-without-ns (strip-ns-form code-str)]
      (js/console.log "Evaluating player code...")
      (js/console.log "Code length:" (count code-without-ns))
      (js/console.log "First 200 chars of stripped code:" (subs code-without-ns 0 (min 200 (count code-without-ns))))
      ;; Log full code to help debug symbol resolution issues
      (js/console.log "=== FULL STRIPPED CODE ===")
      (js/console.log code-without-ns)
      (js/console.log "=== END FULL CODE ===")
      (try
        ;; Evaluate the code in the SCI context
        (sci/eval-string* ctx code-without-ns)
        (js/console.log "Code evaluated successfully")
        (catch :default e
          (js/console.error "Error during code evaluation:" e)
          (js/console.error "Error message:" (ex-message e))
          (js/console.error "Error type:" (type e))
          (js/console.error "Error keys:" (js/Object.keys e))
          (when (.-data e)
            (let [data (.-data e)
                  error-line (aget data "C" 3)  ;; Line number from error data
                  error-col (aget data "C" 5)]  ;; Column number from error data
              (js/console.error "Error data:" data)
              (js/console.error "Error data type:" (type data))
              (js/console.error "Error data keys:" (js/Object.keys data))
              (js/console.error "Error data as JSON:" (js/JSON.stringify data nil 2))
              ;; Show the specific line where the error occurred
              (when error-line
                (let [lines (clojure.string/split code-without-ns #"\n")
                      error-line-idx (dec error-line)
                      problematic-line (nth lines error-line-idx nil)]
                  (js/console.error "=== ERROR AT LINE" error-line "COLUMN" error-col "===")
                  (js/console.error problematic-line)
                  (when error-col
                    (js/console.error (str (apply str (repeat (dec error-col) " ")) "^")))))))
          (throw e)))

      ;; Try to get the decide function from the context
      (let [decide-fn (try
                        (sci/eval-string* ctx "decide")
                        (catch :default e
                          (js/console.error "Couldn't find decide function:" e)
                          nil))]
        (if (fn? decide-fn)
          (do
            (js/console.log "Successfully extracted decide function")
            decide-fn)
          (do
            (js/console.error "decide is not a function:" (type decide-fn))
            nil))))
    (catch :default e
      (js/console.error "Error evaluating player code:" e)
      (js/console.error "Error details:" (ex-message e))
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
