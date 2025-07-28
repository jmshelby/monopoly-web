(ns jmshelby.monopoly-web.styles
  (:require-macros
    [garden.def :refer [defcssfn]])
  (:require
    [spade.core   :refer [defglobal defclass]]
    [garden.units :refer [deg px]]
    [garden.color :refer [rgba]]))

(defcssfn linear-gradient
 ([c1 p1 c2 p2]
  [[c1 p1] [c2 p2]])
 ([dir c1 p1 c2 p2]
  [dir [c1 p1] [c2 p2]]))

(defglobal defaults
  [:body
   {:color            "#333"
    :background-color "#f8f9fa"
    :font-family      "Arial, sans-serif"
    :margin           0
    :padding          0}]
  
  [:.btn-primary
   {:background-color "#007bff"
    :border-color     "#007bff"
    :color            "#fff"}]
  
  [:.btn-secondary
   {:background-color "#6c757d"
    :border-color     "#6c757d"
    :color            "#fff"}]
    
  [:.btn-success
   {:background-color "#28a745"
    :border-color     "#28a745"
    :color            "#fff"}]
    
  [:.btn-danger
   {:background-color "#dc3545"
    :border-color     "#dc3545"
    :color            "#fff"}])

(defclass level1
  []
  {:color :green})
