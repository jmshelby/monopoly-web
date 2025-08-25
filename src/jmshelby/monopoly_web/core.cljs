(ns jmshelby.monopoly-web.core
  (:require
   [reagent.dom :as rdom]
   [re-frame.core :as re-frame]
   [clojure.core.async :as async]
   [servant.core :as servant]
   [servant.worker :as worker]
   [jmshelby.monopoly-web.events :as events]
   [jmshelby.monopoly-web.routes :as routes]
   [jmshelby.monopoly-web.views-simple :as views]
   [jmshelby.monopoly-web.config :as config]
   [jmshelby.monopoly-web.styles :as styles]))

(def WORKER-COUNT 1)
(def WORKER-SCRIPT "/js/compiled/worker-simulations.js") ;; This is whatever the name of this script will be

;; Start Up Workers
(println "Starting workers: " WORKER-COUNT WORKER-SCRIPT)
(def worker-channel (servant/spawn-servants WORKER-COUNT WORKER-SCRIPT))
(println "Starting workers...Done")

(defn dev-setup []
  (when config/debug?
    (println "dev mode")))

(defn ^:dev/after-load mount-root []
  (re-frame/clear-subscription-cache!)
  (let [root-el (.getElementById js/document "app")]
    (rdom/unmount-component-at-node root-el)
    (rdom/render [views/simple-main-panel] root-el)))

(defn servant-thread [servant-channel post-message-fn f-key & args]
  (apply servant/servant-thread-with-key servant-channel post-message-fn f-key args))

;;Trying to replicate this for web workers
;;(async/pipeline parallelism output-ch (map process-game) input-ch)
(defn- worker-pipeline
  [worker-ch
   output-ch
   worker-fn
   input-ch]
  ;; Start a loop..
  (async/go-loop []
    ;; Pull from input...
    ;; TODO - close out channel when in chan closed?
    (when-let [in (async/<! input-ch)]
      (println "PIPE: ("  (:game-num in) ") new game available")
      ;; Submit job to next available worker
      (let [worker-resp-ch
            (servant-thread
             worker-ch
             servant/standard-message
             worker-fn
             in)]
        (println "PIPE: ("  (:game-num in) ") sent to worker")
        ;; Setup a background proc to wait on response,
        ;; and feed on to the output channel when ready.
        (async/go
          (let [worker-resp (async/<! worker-resp-ch)]
            (println "PIPE: ("
                     (:game-num in)
                     ") worker response ready, resp game #:"
                     (:game-num (js->clj worker-resp
                                         :keywordize-keys true)))
            (async/>! output-ch
                      worker-resp)))
        ;; Keep going...
        (recur)))))

;; An event to start a bulk simulation of monopoly games
(re-frame/reg-fx
 :monopoly/simulation
 (fn [{:keys [num-games num-players safety-threshold]}]

   (println "FX: SIMULATION START")

   (let [num-players (or num-players 4)
         safety-threshold (or safety-threshold 1000)
         ;; Create channels
         ;; TODO - buffers?
         input-ch (async/chan)
         output-ch (async/chan)]

     ;; Dispatch to save the channel
     (re-frame/dispatch [:jmshelby.monopoly-web.events/bulk-sim-started
                         output-ch])

     ;; Start pipeline to run sims in web workers
     (worker-pipeline worker-channel
                      output-ch
                      :run-game
                      input-ch)

     ;; Start feeding game sim params to input channel
     (async/go
       (doseq [game-num (range num-games)]
         (println "FX: Feeding game in: " game-num "...")
         (async/>! input-ch {:game-num game-num
                             :num-players num-players
                             :safety-threshold safety-threshold})

         (println "FX: Feeding game in: " game-num "...DONE")
         ))

;; Start feeding game results to event
     (async/go-loop []
       (when-let [game (async/<! output-ch)]
         ;; Convert back to clj types
         (let [game (js->clj game :keywordize-keys true)]
           (println "FX (" (:game-num game) ") new game ready, dispatching ...")
           (re-frame/dispatch [:jmshelby.monopoly-web.events/bulk-sim-game-finished
                               game])
           (println "FX (" (:game-num game) ") new game ready, dispatching ...continuing")
           (recur))))

;;
     )))

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





