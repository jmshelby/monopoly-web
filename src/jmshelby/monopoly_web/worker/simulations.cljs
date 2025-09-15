(ns jmshelby.monopoly-web.worker.simulations
  (:require [jmshelby.monopoly.core :as mono-core]
            [jmshelby.monopoly.simulation :as mono-sim]))

(println "Inside *top* of simulation worker script.")

;; OlD stuff with servant lib...
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


(defn run-simulation
  [{:keys [game-num
           num-players safety-threshold]
    :as params}]

  (let [game-state (mono-core/rand-game-end-state
                    num-players safety-threshold)
        ;; Extract minimal stats and let GC clean up the full game state
        analysis (mono-sim/analyze-game-outcome game-state)]
    ;; TODO - Anything else to do with this analysis??
    analysis
    ))

(defn on-message
  [event]
  (let [;; The evenet.data contains the
        ;; params passed from caller thread
        params (.-data event)
        ;; But of course we need to convert
        ;; back to clj data types, don't
        ;; forget the keyword keys
        cljsified (js->clj params :keywordize-keys true)
        ;; Call it...
        clj-result (run-simulation cljsified)
        ;; Make sure we have the js serializable form
        result (clj->js clj-result)]
    ;; Return message
    (.postMessage js/self result)))

;; Just define the message event here
(set! (.-onmessage js/self) on-message)


(println "Inside *bottom* of simulation worker script.")

;;
