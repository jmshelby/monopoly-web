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

(defn- wait-for-next-game
  [chan completion-event]
  (async/go
    (when-let [game (async/<! chan)]
      (println "new game ready, dispatching ...")
      (re-frame/dispatch [completion-event game])
      (println "new game ready, dispatching ...continuing"))))

;; An event to start a bulk simulation of monopoly games
(re-frame/reg-fx
 :monopoly/simulation
 (fn [{:keys [num-games num-players safety-threshold started-event completion-event]}]
   (let [num-players (or num-players 4)
         safety-threshold (or safety-threshold 1000)
         output-ch (core-sim/run-simulation num-games
                                            num-players
                                            safety-threshold)]
     ;; Dispatch to save the channel
     (when started-event
       (re-frame/dispatch [started-event output-ch]))
     ;; Prime the compute cycle with the first game
     (wait-for-next-game output-ch completion-event))))

(re-frame/reg-fx
 :monopoly/simulation-continue
 (fn [{:keys [output-ch completion-event]}]
   ;; Just another take and dispatch when ready
   (wait-for-next-game output-ch completion-event)))

(re-frame/reg-fx
 :monopoly/simulation-stop
 (fn [output-ch]
   ;; Close the channel, and games can't continue
   (println "closed channel!")
   (async/close! output-ch)))

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





