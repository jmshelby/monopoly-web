(ns jmshelby.monopoly-web.worker.simulations
  "Web Worker entry point for bulk simulations. Runs a chunk of games to
  completion off the main thread and streams analyses back via postMessage.

  Inbound message (from main thread):
    {:games N :num-players P :safety-threshold T}
  Outbound messages (to main thread):
    {:type \"results\" :results [analysis ...]}  ;; one per BATCH games
    {:type \"done\"}                              ;; chunk finished"
  (:require [jmshelby.monopoly.core :as mono-core]
            [jmshelby.monopoly.simulation :as mono-sim]))

(def ^:const BATCH
  "Number of game analyses to accumulate before posting a batch back to the
  main thread. Batching keeps postMessage + re-frame dispatch overhead from
  dominating once games stream back in parallel."
  25)

(defn run-chunk
  "Run `games` complete simulations, posting analyses back in batches of BATCH."
  [{:keys [games num-players safety-threshold]}]
  (let [buf    (volatile! [])
        flush! (fn []
                 (when (seq @buf)
                   (.postMessage js/self (clj->js {:type :results :results @buf}))
                   (vreset! buf [])))]
    (dotimes [_ games]
      (let [analysis (-> (mono-core/rand-game-end-state num-players safety-threshold)
                         (mono-sim/analyze-game-outcome))]
        (vswap! buf conj analysis)
        (when (>= (count @buf) BATCH)
          (flush!))))
    (flush!)
    (.postMessage js/self (clj->js {:type :done}))))

(defn on-message [event]
  (run-chunk (js->clj (.-data event) :keywordize-keys true)))

(defn init []
  (set! (.-onmessage js/self) on-message))
