(ns jmshelby.monopoly-web.player-evaluator
  (:require
   [sci.core :as sci]
   [clojure.set :as set]
   [jmshelby.monopoly.util :as util]))

;; Create a SCI context with necessary namespaces and functions
;; available to the evaluated player code
(def sci-context
  (sci/init {:namespaces {'clojure.set {'difference set/difference
                                        'union set/union
                                        'intersection set/intersection}
                          'jmshelby.monopoly.util {'player-by-id util/player-by-id
                                                   'owned-properties util/owned-properties
                                                   'owned-property-details util/owned-property-details
                                                   'street-group-counts util/street-group-counts
                                                   'potential-house-purchases util/potential-house-purchases}}
             :classes {'js js/globalThis}}))

(defn evaluate-player-code
  "Evaluate player code string and return the decide function.
   Returns a map with either:
   - {:success true :decide-fn <fn>} on success
   - {:success false :error <msg>} on error"
  [code-str]
  (try
    ;; Evaluate the code in the SCI context
    (sci/eval-string* sci-context code-str)

    ;; Try to extract the 'decide' function from the context
    (let [decide-fn (sci/eval-string* sci-context "decide")]
      (if (fn? decide-fn)
        {:success true :decide-fn decide-fn}
        {:success false :error "No 'decide' function found in player code"}))

    (catch js/Error e
      {:success false :error (str "Evaluation error: " (.-message e))})))

(defn test-player-code
  "Test the player code by evaluating it and calling decide with test params.
   Returns a map with evaluation results and test call results."
  [code-str]
  (let [eval-result (evaluate-player-code code-str)]
    (if (:success eval-result)
      (try
        ;; Try a simple test call to the decide function
        (let [decide-fn (:decide-fn eval-result)
              ;; Create minimal test game state
              test-game-state {:players [{:id 1 :cash 1500 :properties {}}
                                         {:id 2 :cash 1500 :properties {}}]
                               :board {:properties []}
                               :transactions []}
              ;; Test with a simple :property-option decision
              test-result (decide-fn test-game-state 1 :property-option {})]
          {:success true
           :decide-fn decide-fn
           :test-result test-result
           :message "Player code evaluated and tested successfully"})
        (catch js/Error e
          {:success false
           :error (str "Runtime error during test: " (.-message e))}))
      eval-result)))
