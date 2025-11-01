(ns jmshelby.monopoly-web.player-eval
  (:require
   [sci.core :as sci]
   [clojure.set :as set]
   [jmshelby.monopoly.util :as util]
   [jmshelby.monopoly.core :as monopoly-core]))

;; Create sci context with necessary namespaces and functions
(defn create-sci-context []
  (sci/init {:namespaces {'clojure.set {'difference set/difference
                                        'union set/union
                                        'intersection set/intersection}
                          'jmshelby.monopoly.util {'player-by-id util/player-by-id
                                                   'owned-properties util/owned-properties
                                                   'owned-property-details util/owned-property-details
                                                   'street-group-counts util/street-group-counts
                                                   'potential-house-purchases util/potential-house-purchases}}
             :classes {'js goog/global :allow :all}}))

(defn eval-player-code
  "Evaluates player code string and returns the sci context with the evaluated code.
   Returns a map with :success true/false and :context or :error"
  [code-string]
  (try
    (let [ctx (create-sci-context)
          _ (sci/eval-string* ctx code-string)]
      {:success true :context ctx})
    (catch :default e
      {:success false :error (str "Evaluation error: " (.-message e))})))

(defn extract-decide-fn
  "Extracts the decide function from the evaluated sci context.
   Returns a map with :success true/false and :decide-fn or :error"
  [sci-ctx]
  (try
    (let [decide-fn (sci/eval-string* sci-ctx "decide")]
      (if decide-fn
        {:success true :decide-fn decide-fn}
        {:success false :error "No 'decide' function found in player code"}))
    (catch :default e
      {:success false :error (str "Error extracting decide function: " (.-message e))})))

(defn wrap-player-fn
  "Wraps the sci-evaluated decide function to work with the monopoly game engine.
   The wrapped function calls the sci-evaluated function within the sci context."
  [sci-ctx decide-fn]
  (fn [game-state player-id method params]
    (try
      ;; Call the decide function with the sci context
      (sci/eval-string* sci-ctx
                       (str "(decide "
                            (pr-str game-state) " "
                            (pr-str player-id) " "
                            (pr-str method) " "
                            (pr-str params) ")"))
      (catch :default e
        (js/console.error "Error calling player decide function:" (.-message e))
        ;; Return a safe default action
        {:action :done}))))

(defn create-player-from-code
  "Creates a player function from code string.
   Returns a map with :success true/false and :player-fn or :error"
  [code-string]
  (let [{:keys [success context error]} (eval-player-code code-string)]
    (if-not success
      {:success false :error error}
      (let [{:keys [success decide-fn error]} (extract-decide-fn context)]
        (if-not success
          {:success false :error error}
          {:success true
           :player-fn (wrap-player-fn context decide-fn)
           :context context})))))
