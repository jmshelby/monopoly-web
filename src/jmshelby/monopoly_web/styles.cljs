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
    :font-family      "\"Courier New\", \"Monaco\", \"Consolas\", monospace !important"
    :font-weight      "normal !important"
    :font-size        "13pt !important"
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
    :font-family "\"Courier New\", \"Monaco\", \"Consolas\", monospace"
    :font-weight "bold"
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
    :font-family "\"Courier New\", \"Monaco\", \"Consolas\", monospace"
    :font-weight "normal"
    :font-size "13pt"
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

  [:.btn-info
   {:background-color "#0099cc"
    :border-color     "#0099cc"
    :color            "#ffffff"}
   [:&:hover
    {:background-color "#00ccff"
     :border-color     "#00ccff"}]]

  [:input :select
   {:background-color "transparent"
    :border "1px solid #666666"
    :color "#cccccc"
    :font-family "\"Courier New\", \"Monaco\", \"Consolas\", monospace"
    :font-weight "normal"
    :font-size "13pt"
    :padding "0.3em 0.5em"}
   [:&:focus
    {:border-color "#009900"
     :outline "none"
     :box-shadow "0 0 3px #009900"}]]

  [:.code-block
   {:background-color "#10101a"
    :border "1px solid #333340"
    :color "#cccccc"
    :font-family "\"Courier New\", \"Monaco\", \"Consolas\", monospace"
    :font-weight "normal"
    :padding "1em"
    :overflow "auto"
    :white-space "pre-wrap"}]

  [:.highlight
   {:color "#ffff66"
    :background-color "transparent"}]

  ;; Player Lab Styles
  [:.player-lab-container
   {:display "flex"
    :height "100vh"}]

  [:.player-lab-left-panel
   {:width "50%"
    :border-right "1px solid #333340"
    :display "flex"
    :flex-direction "column"}]

  [:.player-lab-header
   {:padding "1em"
    :border-bottom "1px solid #333340"
    :background-color "#0f0f23"}
   [:h3
    {:margin "0 0 1em 0"}]]

  [:.player-lab-buttons
   {:display "flex"
    :gap "1em"}]

  [:.player-lab-editor-area
   {:flex "1"
    :padding "1em"}
   [:h4
    {:margin-bottom "0.5em"}]]

  [:.player-lab-textarea
   {:width "100%"
    :height "400px"
    :font-family "\"Courier New\", \"Monaco\", \"Consolas\", monospace"
    :font-size "14px"
    :border "1px solid #333340"
    :padding "0"
    :background-color "#10101a"
    :color "#cccccc"
    :resize "none"}
   [:&:focus
    {:border-color "#009900"
     :outline "none"
     :box-shadow "0 0 3px #009900"}]]

  ;; CodeMirror 6 container styling (oneDark theme handles most styling)
  [:.cm-editor
   {:height "400px !important"
    :font-family "\"Courier New\", \"Monaco\", \"Consolas\", monospace !important"
    :font-size "14px !important"
    :border "1px solid #333340 !important"}]
  
  [:.cm-focused
   {:outline "none !important"
    :border-color "#009900 !important"
    :box-shadow "0 0 3px #009900 !important"}]

  [:.player-lab-right-panel
   {:width "50%"
    :padding "2em"
    :background-color "#0f0f23"}]

  [:.player-lab-placeholder
   {:border "2px dashed #333340"
    :padding "2em"
    :text-align "center"
    :margin-top "2em"
    :color "#666666"
    :background-color "#10101a"}
   [:p
    {:margin "0.5em 0"}]])

(defclass level1
  []
  {:color :green})
