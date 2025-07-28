(ns jmshelby.monopoly-web.subs
  (:require
   [re-frame.core :as re-frame]))

(re-frame/reg-sub
 ::active-panel
 (fn [db _]
   (:active-panel db)))

;; Game setup subscriptions
(re-frame/reg-sub
 ::game-mode
 (fn [db _]
   (get-in db [:game-setup :mode])))

(re-frame/reg-sub
 ::players
 (fn [db _]
   (get-in db [:game-setup :players])))

;; Single game subscriptions
(re-frame/reg-sub
 ::single-game-state
 (fn [db _]
   (get-in db [:single-game :state])))

(re-frame/reg-sub
 ::single-game-running?
 (fn [db _]
   (get-in db [:single-game :running?])))

(re-frame/reg-sub
 ::game-log
 (fn [db _]
   (get-in db [:single-game :log])))

;; Bulk simulation subscriptions
(re-frame/reg-sub
 ::bulk-sim-running?
 (fn [db _]
   (get-in db [:bulk-simulation :running?])))

(re-frame/reg-sub
 ::bulk-sim-progress
 (fn [db _]
   (get-in db [:bulk-simulation :progress])))

(re-frame/reg-sub
 ::bulk-sim-total
 (fn [db _]
   (get-in db [:bulk-simulation :total-games])))

(re-frame/reg-sub
 ::bulk-sim-results
 (fn [db _]
   (get-in db [:bulk-simulation :results])))

(re-frame/reg-sub
 ::bulk-sim-config
 (fn [db _]
   (get-in db [:bulk-simulation :config])))

;; UI state subscriptions
(re-frame/reg-sub
 ::loading?
 (fn [db _]
   (:loading? db)))

(re-frame/reg-sub
 ::error-message
 (fn [db _]
   (:error-message db)))
