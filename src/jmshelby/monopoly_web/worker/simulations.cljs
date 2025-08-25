(ns jmshelby.monopoly-web.worker.simulations
  (:require [servant.worker :as worker]
            [jmshelby.monopoly.core :as mono-core]
            [jmshelby.monopoly.simulation :as mono-sim])
  (:require-macros [servant.macros :refer [defservantfn]]))

;; TODO - Anything to do here?
(defn init []
  (println "Inside init of of simulation worker..."))

(defservantfn run-game
  [_game-num {:keys [num-players safety-threshold]
              :as game-params}]
  ;; Run a random game
  (let [game-state (mono-core/rand-game-end-state num-players safety-threshold)]
    ;; Extract minimal stats and let GC clean up the full game state
    (mono-sim/analyze-game-outcome game-state)))

(println "Inside worker script, bootstrapping...")
(worker/bootstrap)
(println "Inside worker script, bootstrapping...Done")


;;
