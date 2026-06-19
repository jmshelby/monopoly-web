(ns jmshelby.monopoly-web.core
  (:require
   [reagent.dom :as rdom]
   [re-frame.core :as re-frame]
   [cljs.reader :as reader]
   [clojure.core.async :as async]
   [jmshelby.monopoly-web.events :as events]
   [jmshelby.monopoly-web.routes :as routes]
   [jmshelby.monopoly-web.views-simple :as views]
   [jmshelby.monopoly-web.config :as config]
   [jmshelby.monopoly-web.styles :as styles]
   [jmshelby.monopoly.simulation :as core-sim]
   [jmshelby.monopoly-web.player-eval :as player-eval]))

(defn dev-setup []
  (when config/debug?
    (println "dev mode")))

(defn ^:dev/after-load mount-root []
  (re-frame/clear-subscription-cache!)
  (let [root-el (.getElementById js/document "app")]
    (rdom/unmount-component-at-node root-el)
    (rdom/render [views/simple-main-panel] root-el)))

;; Bulk simulation runs each game in a Web Worker so CPU-bound games execute
;; in parallel across cores instead of one-at-a-time on the main JS thread.
(def ^:const WORKER-SCRIPT "/js/compiled/worker-simulations.js")

(defn- worker-count
  "Number of workers to spawn: one per core, minus one to keep the main thread
  responsive. Never more than the number of games, never less than one."
  [num-games]
  (let [cores (or (.-hardwareConcurrency js/navigator) 4)]
    (max 1 (min num-games (dec cores)))))

(defn- split-games
  "Split `num-games` into `n` chunk sizes, spreading the remainder over the
  first few chunks. Returns a seq of positive chunk sizes."
  [num-games n]
  (let [base  (quot num-games n)
        extra (rem num-games n)]
    (->> (range n)
         (map #(+ base (if (< % extra) 1 0)))
         (remove zero?))))

;; An event to start a bulk simulation of monopoly games
(re-frame/reg-fx
 :monopoly/simulation
 (fn [{:keys [num-games num-players safety-threshold started-event completion-event]}]
   (let [num-players      (or num-players 4)
         safety-threshold (or safety-threshold 1000)
         chunks           (split-games num-games (worker-count num-games))
         workers
         (mapv
          (fn [chunk]
            (let [w (js/Worker. WORKER-SCRIPT)]
              ;; Each worker streams back batches of finished game analyses
              (set! (.-onmessage w)
                    (fn [e]
                      (let [msg (reader/read-string (.-data e))]
                        (when (= :results (:type msg))
                          (re-frame/dispatch [completion-event (:results msg)])))))
              (.postMessage w (pr-str {:games            chunk
                                       :num-players      num-players
                                       :safety-threshold safety-threshold}))
              w))
          chunks)]
     ;; Hand the workers back so they can be terminated on stop
     (when started-event
       (re-frame/dispatch [started-event workers])))))

(re-frame/reg-fx
 :monopoly/simulation-stop
 (fn [workers]
   ;; Terminate all workers so in-flight games stop producing results
   (doseq [w workers]
     (.terminate w))))

;; Custom player simulation effect
(re-frame/reg-fx
 :monopoly/custom-player-simulation
 (fn [{:keys [num-games num-players safety-threshold player-code started-event completion-event]}]
   (let [num-players (or num-players 4)
         safety-threshold (or safety-threshold 1000)]
     (println "Starting custom player simulation with code...")

     ;; Try to evaluate the player code
     (if-let [player-fn (player-eval/create-player-fn player-code)]
       (do
         (println "Player code evaluated successfully")
         ;; Dispatch started event
         (when started-event
           (re-frame/dispatch [started-event]))

         ;; Run games asynchronously
         (async/go
           (dotimes [i num-games]
             (try
               (println "Running game" (inc i) "of" num-games)
               (let [_          (println "Calling run-custom-game...")
                     game-state (player-eval/run-custom-game player-fn num-players safety-threshold)
                     _          (println "Game completed, analyzing...")
                     analysis   (core-sim/analyze-game-outcome game-state)
                     ;; Enhance analysis with custom player tracking
                     custom-player-id (:custom-player-id game-state)
                     custom-player-won? (and (:has-winner analysis)
                                            (= custom-player-id (:winner-id analysis)))
                     enhanced-analysis (assoc analysis
                                             :custom-player-id custom-player-id
                                             :custom-player-won custom-player-won?)]
                 (println "Analysis complete, dispatching results...")
                 ;; Dispatch completion event for this game
                 (re-frame/dispatch [completion-event enhanced-analysis])
                 (println "Game" (inc i) "finished successfully")
                 ;; Yield to the event loop so UI can update
                 (async/<! (async/timeout 0)))
               (catch :default e
                 (js/console.error "Error running custom game:" e)
                 (js/console.error "Error stack:" (.-stack e)))))))
       ;; If evaluation failed, dispatch error
       (do
         (println "Failed to evaluate player code")
         (re-frame/dispatch [:jmshelby.monopoly-web.events/set-error
                             "Failed to evaluate player code. Check console for errors."])
         (re-frame/dispatch [:jmshelby.monopoly-web.events/set-player-lab-running false]))))))

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





