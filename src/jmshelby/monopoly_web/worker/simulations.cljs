(ns jmshelby.monopoly-web.worker.simulations
  "Web Worker entry point for bulk simulations. Runs a chunk of games to
  completion off the main thread and streams analyses back via postMessage.

  Messages are EDN strings (pr-str / read-string) so Clojure data round-trips
  losslessly across the worker boundary (keyword values and exact integers
  survive, unlike clj->js/js->clj).

  Inbound message (from main thread):
    {:games N :num-players P :safety-threshold T}
  Outbound messages (to main thread):
    {:type :results :results [analysis ...]}  ;; one per BATCH games
    {:type :done}                             ;; chunk finished"
  (:require [cljs.reader :as reader]
            [jmshelby.monopoly.core :as mono-core]
            [jmshelby.monopoly.simulation :as mono-sim]))

(def ^:const BATCH
  "Number of game analyses to accumulate before posting a batch back to the
  main thread. Batching keeps postMessage + re-frame dispatch overhead from
  dominating once games stream back in parallel."
  25)

(def ^:const SAMPLE-CAP
  "Per-game cap on the raw auction-transaction vectors that analyze-game-outcome
  attaches. These vectors are only ever used to render a handful of sample rows
  (calculate-statistics takes 5 games x 10 txns), but for long games they can be
  large. Serializing thousands of them across the worker boundary is what spikes
  main-thread memory, so we keep only a few entries per game here. Every numeric
  statistic is derived from separate scalar count fields and is unaffected."
  5)

(def ^:private sample-keys
  "Display-only, unbounded fields on each analysis to trim before serializing."
  [:auction-initiated-transactions
   :auction-completed-transactions
   :auction-passed-transactions
   :property-declined-auctions
   :bankruptcy-auctions])

(defn- slim
  "Cap the display-only transaction vectors so message payloads (and retained
  results) stay small. Leaves all scalar/count fields untouched."
  [analysis]
  (reduce (fn [a k] (update a k #(into [] (take SAMPLE-CAP) %)))
          analysis
          sample-keys))

(defn- post! [msg]
  (.postMessage js/self (pr-str msg)))

(defn run-chunk
  "Run `games` complete simulations, posting analyses back in batches of BATCH."
  [{:keys [games num-players safety-threshold]}]
  (let [buf    (volatile! [])
        flush! (fn []
                 (when (seq @buf)
                   (post! {:type :results :results @buf})
                   (vreset! buf [])))]
    (dotimes [_ games]
      (let [analysis (-> (mono-core/rand-game-end-state num-players safety-threshold)
                         (mono-sim/analyze-game-outcome)
                         (slim))]
        (vswap! buf conj analysis)
        (when (>= (count @buf) BATCH)
          (flush!))))
    (flush!)
    (post! {:type :done})))

(defn on-message [event]
  (run-chunk (reader/read-string (.-data event))))

(defn init []
  (set! (.-onmessage js/self) on-message))
