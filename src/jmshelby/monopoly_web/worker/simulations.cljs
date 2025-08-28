(ns jmshelby.monopoly-web.worker.simulations
  (:require [jmshelby.monopoly.core :as mono-core]
            [jmshelby.monopoly.simulation :as mono-sim]))

(println "Inside *top* of simulation worker script.")

;; TODO - Anything to do here?
;; TODO - who is declaring this???
(defn init []
  (println "Inside init of of simulation worker..."))

;; (defservantfn run-game
;;   [_game-num {:keys [num-players safety-threshold]
;;               :as game-params}]
;;   ;; Run a random game
;;   (let [game-state (mono-core/rand-game-end-state num-players safety-threshold)]
;;     ;; Extract minimal stats and let GC clean up the full game state
;;     (mono-sim/analyze-game-outcome game-state)))

;; (println "Inside worker script, bootstrapping...")
;; (worker/bootstrap)
;; (println "Inside worker script, bootstrapping...Done")


(println "Inside *bottom* of simulation worker script.")

;;
