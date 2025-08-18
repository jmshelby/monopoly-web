(ns jmshelby.monopoly-web.db)

(def default-db
  {:active-panel :battle-opoly
   :route-params {}

   ;; Game setup configuration
   :game-setup {:mode nil
                :players []}

   ;; Single game state and results
   :single-game {:state nil
                 :running? false
                 :summary nil
                 :log []}

   ;; Bulk simulation state and results
   :bulk-simulation {:running? false
                     :progress 0
                     :total-games 0
                     :results nil
                     :config {}}

   ;; UI state
   :loading? false
   :error-message nil})
