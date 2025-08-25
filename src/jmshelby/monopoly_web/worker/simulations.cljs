(ns jmshelby.monopoly-web.worker.simulations
  (:require [servant.worker :as worker]
            [jmshelby.monopoly.core :as mono-core]
            [jmshelby.monopoly.simulation :as mono-sim]
            [jmshelby.monopoly.analysis :as analysis])
  (:require-macros [servant.macros :refer [defservantfn]]))

;; TODO - Anything to do here?
(defn init []
  (println "Inside init of of simulation worker..."))

(defservantfn run-game
  [params]
  (let [;; Parse params as a clojure type
        {:keys [game-num
                num-players safety-threshold]
         :as params}
        (js->clj params :keywordize-keys true)
        ;; Play a random game
        game-state (mono-core/rand-game-end-state
                    num-players safety-threshold)
        ;; Extract minimal stats and let GC clean up the full game state
        analysis (mono-sim/analyze-game-outcome game-state)]
    (println "WORKER: " game-num " ran game")
    ;; Need to return as a javascript object
    ;; TODO - too bad this isn't abstracted for us...
    (clj->js analysis)))

;; TODO - check if we're in a web worker?
(worker/bootstrap)

;;
