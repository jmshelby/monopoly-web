(ns jmshelby.monopoly-web.worker)

(defn ^:export init []
  (js/console.log "Game simulation worker initialized"))

(defn run-bulk-simulation [game-config num-simulations]
  (js/console.log (str "Running " num-simulations " game simulations"))
  ;; Placeholder for actual simulation logic
  {:results []
   :total-simulations num-simulations
   :completed true})

(defn ^:export handle-message [event]
  (let [data (.-data event)
        message-type (.-type data)]
    (case message-type
      "run-simulations"
      (let [config (.-config data)
            num-sims (.-numSimulations data)
            results (run-bulk-simulation config num-sims)]
        (js/postMessage (clj->js {:type "simulation-complete"
                                  :results results})))

      "ping"
      (js/postMessage (clj->js {:type "pong"}))

      (js/console.warn "Unknown message type:" message-type))))

(js/addEventListener "message" handle-message)
