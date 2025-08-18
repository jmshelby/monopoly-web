(ns jmshelby.monopoly-web.core
  (:require
   [reagent.dom :as rdom]
   [re-frame.core :as re-frame]
   [clojure.core.async :as async]
   [jmshelby.monopoly-web.events :as events]
   [jmshelby.monopoly-web.routes :as routes]
   [jmshelby.monopoly-web.views-simple :as views]
   [jmshelby.monopoly-web.config :as config]
   [jmshelby.monopoly-web.styles :as styles]
   [jmshelby.monopoly.simulation :as core-sim]))

(defn dev-setup []
  (when config/debug?
    (println "dev mode")))

(defn ^:dev/after-load mount-root []
  (re-frame/clear-subscription-cache!)
  (let [root-el (.getElementById js/document "app")]
    (rdom/unmount-component-at-node root-el)
    (rdom/render [views/simple-main-panel] root-el)))

;; An event to start a bulk simulation of monopoly games
(re-frame/reg-fx
 :monopoly/simulation
 (fn [{:keys [num-games num-players safety-threshold]}]

   (let [num-players (or num-players 4)
         safety-threshold (or safety-threshold 1500)
         _ (println "Starting a bulk game simulation session...")
         output-ch (core-sim/run-simulation num-games
                                            num-players
                                            safety-threshold)
         _ (println "Starting a bulk game simulation session...Started")]

     (println "!GO! BEFORE")
     (async/go
       (println "Going to wait for the next game...")
       (let [game (async/<! output-ch)
             event :jmshelby.monopoly-web.events/bulk-sim-game-finished]
         (println "new game ready, dispatching ...")
         (re-frame/dispatch [event game])))
     (println "!GO! AFTER")

;;
     )))


(defn init []
  (try
    (println "Starting monopoly-web application...")
    (routes/start!)
    (println "Routes started")
    (re-frame/dispatch-sync [::events/initialize-db])
    (println "Database initialized")
    (dev-setup)
    (println "Dev setup complete")
    (mount-root)
    (println "Root mounted")
    (catch js/Error e
      (println "Error during init:" e)
      (js/console.error "Monopoly-web init error:" e))))





