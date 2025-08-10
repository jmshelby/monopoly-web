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
   {:color            "#cccccc !important"
    :background-color "#0f0f23 !important"
    :font-family      "\"Source Code Pro\", monospace !important"
    :font-weight      "300 !important"
    :font-size        "14pt !important"
    :margin           "0 !important"
    :padding          "0 !important"
    :min-width        "60em"}]
  
  [:html
   {:background-color "#0f0f23 !important"}]
  
  [:#app
   {:background-color "#0f0f23 !important"
    :color            "#cccccc !important"
    :min-height       "100vh"}]
  
  [:h1 :h2 :h3 :h4 :h5 :h6
   {:color "#00cc00"
    :font-family "\"Source Code Pro\", monospace"
    :font-weight "300"
    :text-shadow "0 0 2px #00cc00, 0 0 5px #00cc00"}]
    
  [:a
   {:color "#009900"
    :text-decoration "none"
    :transition "color 0.2s ease"}
   [:&:hover
    {:color "#99ff99"}]]
  
  [:button
   {:background-color "#10101a"
    :border "1px solid #333340"
    :color "#cccccc"
    :font-family "\"Source Code Pro\", monospace"
    :font-weight "300"
    :font-size "14pt"
    :padding "0.5em 1em"
    :cursor "pointer"
    :transition "all 0.2s ease"}
   [:&:hover
    {:background-color "#1a1a2e"
     :border-color "#666666"
     :color "#ffffff"}]]
  
  [:.btn-primary
   {:background-color "#009900"
    :border-color     "#009900"
    :color            "#ffffff"}
   [:&:hover
    {:background-color "#00cc00"
     :border-color     "#00cc00"}]]
  
  [:.btn-secondary
   {:background-color "#333340"
    :border-color     "#666666"
    :color            "#cccccc"}
   [:&:hover
    {:background-color "#4a4a5a"
     :border-color     "#999999"}]]
    
  [:.btn-success
   {:background-color "#009900"
    :border-color     "#009900"
    :color            "#ffffff"}
   [:&:hover
    {:background-color "#00cc00"
     :border-color     "#00cc00"}]]
    
  [:.btn-danger
   {:background-color "#cc3333"
    :border-color     "#cc3333"
    :color            "#ffffff"}
   [:&:hover
    {:background-color "#ff3333"
     :border-color     "#ff3333"}]]
     
  [:input :select
   {:background-color "transparent"
    :border "1px solid #666666"
    :color "#cccccc"
    :font-family "\"Source Code Pro\", monospace"
    :font-weight "300"
    :font-size "14pt"
    :padding "0.3em 0.5em"}
   [:&:focus
    {:border-color "#009900"
     :outline "none"
     :box-shadow "0 0 3px #009900"}]]
     
  [:.code-block
   {:background-color "#10101a"
    :border "1px solid #333340"
    :color "#cccccc"
    :font-family "\"Source Code Pro\", monospace"
    :font-weight "300"
    :padding "1em"
    :overflow "auto"}]
    
  [:.highlight
   {:color "#ffff66"
    :background-color "transparent"}])

(defclass level1
  []
  {:color :green})
