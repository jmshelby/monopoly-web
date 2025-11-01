(ns jmshelby.monopoly-web.player-evaluator)

;; Placeholder for code evaluation - SCI integration coming soon
(defn evaluate-player-code
  "Evaluate player code string and return the decide function.
   Returns a map with either:
   - {:success true :decide-fn <fn>} on success
   - {:success false :error <msg>} on error"
  [code-str]
  ;; For now, just return an error indicating this feature is not yet implemented
  {:success false
   :error "Code evaluation not yet implemented - SCI integration in progress"})

(defn test-player-code
  "Test the player code by evaluating it and calling decide with test params.
   Returns a map with evaluation results and test call results."
  [code-str]
  ;; Just call evaluate-player-code for now
  (evaluate-player-code code-str))
