(ns catnip.core
  (:require [reagent.core :as r :refer [atom]]
            [reagent.session :as session]
            [secretary.core :as secretary :include-macros true]))

;; -------------------------
;; State information

;; Keep track the state of the options
(def state (r/atom {:max 2 :speed 3 :showui true :bgimg ""}))

;; Keep a timestamp of the last time the screen was rendered
(def last-ts (r/atom nil))

;; Keep track of all the birds
(def birds (r/atom []))

;; The path for the image of the bird
(def bird-img "sprite/blue-jay-sprite-sheet.png")
(def bird-width 100)
(def bird-height 100)
(def bird-frames 8)

;; -------------------------
;; Reagent components

(defn bird-sprite [bird]
  (let [frame (int (:frame bird))
        offset-max (* bird-width (inc frame))
        offset-min (* bird-width frame)
        scale (if (neg? (:dx bird))
                -1
                1)
        move (if (neg? (:dx bird))
               (- offset-max (* bird-width bird-frames))
               (- offset-min))
        rotate (if (neg? (:dx bird))
                 (* 3 (:dy bird))
                 (* -3 (:dy bird)))]
    ^{:key (:ts bird)}
    [:div {:style {:position  "fixed"
                   :top       (:y bird)
                   :left      (:x bird)
                   :transform (str "rotate(" (str rotate) "deg)")
                   }}
     [:img {:style {:position  "absolute"
                    :clip      (str "rect(0px, " (str offset-max) "px, "
                                    (str bird-height) "px, " (str offset-min) "px)")
                    :transform (str "matrix(" (str scale) ", 0, 0, 1, " (str move) ", 0)")}
            :src   bird-img}]]))

(defn slider [param value min max]
  [:input {:type      "range" :value value :min min :max max
           :style     {:width "90%"}
           :on-change (fn [e]
                        (swap! state assoc param (.-target.value e)))}])

(defn catnip-ui []
  [:div (map bird-sprite @birds)
   [:div {:class "bottom"}
    [:button {:style    {:position "fixed"
                         :bottom   10
                         :right    10
                         :width    100}
              :class    "ui"
              :on-click #(swap! state assoc :showui (not (:showui @state)))}
     (if (:showui @state)
       "Hide options"
       "Show options")]
    [:table {:class "options ui"
             :style {:visibility (if (:showui @state)
                                   "visible"
                                   "hidden")}}
     [:tr
      [:td [:strong "Options"]]
      [:td {:col-span 2
            :style    {:text-align "right"}}
       "Press F11 for fullscreen"]]
     [:tr
      [:td "Birds: "]
      [:td [slider :max (:max @state) 1 10]]
      [:td (:max @state)]]
     [:tr
      [:td [:div "Speed: "]
       [:td [slider :speed (:speed @state) 1 10]]
       [:td (:speed @state)]]
      ;[:div "Background: " (:bgimg @state) ""] ; Need to implement a file chooser for the background
      ]]]]
  )

;; -------------------------
;; Functions

;; Get the bounds that the birds can fly in
(defn get-bounds []
  (let [width (.-innerWidth js/window)
        height (.-innerHeight js/window)]
    {:top (- bird-height) :left (- bird-width) :bottom height :right width}))

;; determines if a bird is out of bounds
(defn out-of-bounds? [bird]
  (let [bounds (get-bounds)]
    (cond
      (< (:x bird) (:left bounds)) true
      (> (:x bird) (:right bounds)) true
      (< (:y bird) (:top bounds)) true
      (> (:y bird) (:bottom bounds)) true
      :else false)))

;; Sleep
(defn sleep [msec]
  (let [deadline (+ msec (.getTime (js/Date.)))]
    (while (> deadline (.getTime (js/Date.))))))

;; Get the body of the DOM
(defn get-body []
  (.-body js/document))

;; Get the DIV with id "app" in the DOM
(defn get-app []
  (.getElementById js/document "app"))

;; Sets the body's background image
(defn set-bg! [imgsrc]
  (do
    (swap! state assoc :bgimg imgsrc)
    (set! (.-backgroundImage (.-style (get-body))) (str "url(\"" imgsrc "\")"))))

;; Update a bird's position
(defn update-bird [delta bird]
  (let [speed (/ (* delta (:speed @state)) 20)]
  {:x     (+ (:x bird) (* speed (:dx bird)))
   :y     (+ (:y bird) (* speed (:dy bird)))
   :dx    (:dx bird)
   :dy    (:dy bird)
   :frame (rem (+ (/ delta 60) (:frame bird)) bird-frames)
   :ts    (:ts bird)}))

;; Move the birds to their next position
(defn next-birds! [delta]
  (reset! birds (map #(update-bird delta %) @birds)))

;; Create a new bird
(defn new-bird []
  (let [bounds (get-bounds)
        dx (* (rand-nth [1 -1]) (inc (rand-int 3)))
        dy (- (rand-int 7) 3)
        x (if (neg? dx) (:right bounds) (:left bounds))
        y (- (rand-int (:bottom bounds)) bird-height)
        f (rand-int bird-frames)]
    {:x x :y y :dx dx :dy dy :frame f :ts (.getTime (js/Date.))}))

;; Add new birds if the current number of birds is less than the max number of birds
(defn add-birds! []
  (let [x (- (:max @state) (count @birds))]
    (cond (= x 1) (reset! birds (conj @birds (new-bird)))
          (> x 1) (do (reset! birds (conj @birds (new-bird)))
                      ;need to wait a ms before makeing the next bird for ts to be unique
                      (sleep 1)
                      (recur)))))

;; Remove birds that have gone off screen
(defn remove-out-of-bounds! []
  (reset! birds (remove out-of-bounds? @birds)))

;; Update the scene
(defn update! [ts]
  (next-birds! (- ts (or @last-ts ts)))
  (remove-out-of-bounds!)
  (add-birds!)
  (reset! last-ts ts)
  (. js/window (requestAnimationFrame update!)))

;; -------------------------
;; Initialize app

(defn mount-root []
  (do
    (set-bg! "bg/nature-forest-waterfall-jungle.jpg")
    (add-birds!)
    (r/render [catnip-ui] (get-app))
    (. js/window (requestAnimationFrame update!))
    ))

(defn init! []
  (mount-root))