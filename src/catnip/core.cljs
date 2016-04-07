(ns catnip.core
  (:require [reagent.core :as r :refer [atom]]
            [reagent.session :as session]
            [secretary.core :as secretary :include-macros true]
            [catnip.fullscreen :as fullscreen]))

;; -------------------------
;; State information

;; options is:
;; {:max Int :speed Int :showui Bool :bgimg String :bgloaded Bool}
;;    - :max      is the maxiumum number of birds that can be on screen at a time
;;    - :speed    is a scaling factor for the spped the birds fly
;;    - :showui   is a flag for determining if the UI elements should be shown or not
;;    - :bgimg    is the path to the background image
;;    - :bgloaded lets the program know if the browser has finished loading the bgimg yet
(defonce options (r/atom {:max      2 :speed 3 :showui true
                          :bgimg    "bg/nature-forest-waterfall-jungle.jpg"
                          :bgloaded false}))

;; Keep a timestamp of the last time the screen was rendered
(defonce last-ts (r/atom nil))

;; birds is (vectorOf Bird)
;; a Bird is:
;; {:key Symbol :x Int :y Int :dx Int :dy Int :frame Int}
;;    - :key   is a unique key for the bird
;;    - :x     is the position of the bird on the X axis
;;    - :y     is the position of the bird on the Y axis
;;    - :dx    is the speed of the bird on the X axis
;;    - :dy    is the speed of the bird on the Y axis
;;    - :frame is the frame of the sprite that should be shown
(defonce birds (r/atom []))

;; bird-sprite is:
;; {:img String :width Int :height Int :frames Int :loaded Bool}
;;    - :img    is the path to the the sprite sheet
;;    - :width  is the width of the sprite
;;    - :height is the height of the sprite
;;    - :frames is the number of fames in the sprite sheet
;;    - :loaded lets the program know if the img is loaded yet
(defonce bird-sprite (r/atom {:img   "sprite/blue-jay-sprite-sheet.png"
                              :width 100 :height 100 :frames 8 :loaded false}))

;; -------------------------
;; Reagent components

;; [] -> Component
;; produce the loading box at the center of the screen
(defn splash-ui []
  [:div.splash
   [:div.ui "Catnip"
    [:div "Loading"
     [:img {:src   (:img @bird-sprite)
            :style {:display "none"}
            :load  (swap! bird-sprite assoc :loaded true)}]
     [:img {:src   (:bgimg @options)
            :style {:display "none"}
            :load  (swap! options assoc :bgloaded true)}]]]])

;; Bird -> Component
;; produce a div positioned at the bird's x and y co-ordinates. Inside the div is an img showing the current frame
;; of the bird sprite. The :key from the bird is put in the meta data of the div so that Reagent can tell which bird
;; is which.
(defn bird-component [bird]
  (let [frame (int (:frame bird))
        offset-max (* (:width @bird-sprite) (inc frame))
        offset-min (* (:width @bird-sprite) frame)
        scale (if (neg? (:dx bird))
                -1
                1)
        move (if (neg? (:dx bird))
               (- offset-max (* (:width @bird-sprite) (:frames @bird-sprite)))
               (- offset-min))
        rotate (if (neg? (:dx bird))
                 (* 3 (:dy bird))
                 (* -3 (:dy bird)))]
    ^{:key (:key bird)}
    [:div {:style {:position  "fixed"
                   :top       (:y bird)
                   :left      (:x bird)
                   :transform (str "rotate(" (str rotate) "deg)")}}

     [:img {:style {:position  "absolute"
                    :clip      (str "rect(0px, " (str offset-max) "px, "
                                    (str (:height @bird-sprite)) "px, " (str offset-min) "px)")
                    :transform (str "matrix(" (str scale) ", 0, 0, 1, " (str move) ", 0)")}
            :src   (:img @bird-sprite)}]]))

;; Atom Key Int Int -> Component
;; produce a slider style input that is at the current value of the atom's param and can range between min and max
;; if the user adjusts the slider the atom's param will be updated to match the current value of the slider
(defn slider [atom param min max]
  [:input {:type      "range" :value (param (deref atom)) :min min :max max
           :style     {:width "90%"}
           :on-change (fn [e]
                        (swap! atom assoc param (.-target.value e)))}])

;; [] -> Component
;; produce the UI for the program
(defn catnip-ui []
  [:div.bg {:style {:backgroundImage (str "url(\"" (:bgimg @options) "\")")}}
   [:div (doall (map bird-component @birds))]
   [:div.bottom
    [:button.ui {:style    {:position "fixed"
                            :bottom   10
                            :left    10
                            :width    100}
                 :on-click #(swap! options assoc :showui (not (:showui @options)))}
     (if (:showui @options)
       "Hide options"
       "Show options")]
    [:button.ui {:style    {:position "fixed"
                            :bottom   10
                            :right     10
                            :width    100}
                 :on-click (fn [e]
                             (.preventDefault e)
                             (fullscreen/toggle (-> e .-currentTarget .-parentNode .-parentNode)))}
     (if (fullscreen/is-fullscreen?)
       "Exit fullscreen"
       "Go fullscreen")]
    [:table.options.ui {
                        :style {:visibility (if (:showui @options)
                                              "visible"
                                              "hidden")}}
     [:tr
      [:td {:col-span 3
            :style    {:text-align "center"}}
       "Options"]]
     [:tr
      [:td "Birds: "]
      [:td [slider options :max (:max @options) 1 10]]
      [:td (:max @options)]]
     [:tr
      [:td [:div "Speed: "]
       [:td [slider options :speed (:speed @options) 1 10]]
       [:td (:speed @options)]]]]]])



;; -------------------------
;; Functions

;; [] -> {:top Int :left Int :bottom Int :right Int}
;; produce a map of the bounds of the screen area with padding for the size of the bird sprite
(defn get-bounds []
  (let [width (.-innerWidth js/window)
        height (.-innerHeight js/window)]
    {:top (- (:height @bird-sprite)) :left (- (:width @bird-sprite)) :bottom height :right width}))

;; Bird -> Bool
;; produce true if the bird's position is outside of the bounds, otherwise false
(defn out-of-bounds? [bird]
  (let [bounds (get-bounds)]
    (cond
      (< (:x bird) (:left bounds)) true
      (> (:x bird) (:right bounds)) true
      (< (:y bird) (:top bounds)) true
      (> (:y bird) (:bottom bounds)) true
      :else false)))

;;; [] -> []
;;; Updates the background image form the one stored in options
(defn apply-bg []
  (set! (.-backgroundImage (.-style (.-body js/document))) (str "url(\"" (:bgimg @options) "\")")))

;;; String -> []
;;; Updates the bgimg of the options and sets the body's background image to that image
(defn set-bg! [imgsrc]
  (do
    (swap! options assoc :bgimg imgsrc)
    (apply-bg)))

;; Int Bird -> Bird
;; produce a new bird with a new position and frame number based on the old bird and the delta of time since the last
;; update
(defn update-bird [delta bird]
  (let [speed (/ (* delta (:speed @options)) 20)]
    {:x     (+ (:x bird) (* speed (:dx bird)))
     :y     (+ (:y bird) (* speed (:dy bird)))
     :dx    (:dx bird)
     :dy    (:dy bird)
     :frame (rem (+ (/ delta 60) (:frame bird)) (:frames @bird-sprite))
     :key   (:key bird)}))

;; Int -> []
;; update the birds atom by updating them all based on the delta of time passed since the last update
(defn next-birds! [delta]
  (reset! birds (map #(update-bird delta %) @birds)))

;; [] -> Bird
;; Create a new bird with a random speed, y position, and frame number. The bird's x postion will be at the right or
;; left edge of the screen depending on it's speed on the x axis.
(defn new-bird []
  (let [bounds (get-bounds)
        dx (* (rand-nth [1 -1]) (inc (rand-int 3)))
        dy (- (rand-int 7) 3)
        x (if (neg? dx) (:right bounds) (:left bounds))
        y (- (rand-int (:bottom bounds)) (:height @bird-sprite))
        f (rand-int (:frames @bird-sprite))]
    {:key (gensym "Bird") :x x :y y :dx dx :dy dy :frame f}))

;; [] -> []
;; add new birds until the number of birds is at the max number of birds in options
(defn add-birds! []
  (if (> (:max @options) (count @birds))
    (do (reset! birds (conj @birds (new-bird)))
        (recur))))

;; [] -> []
;; Remove birds that have gone off screen
(defn remove-out-of-bounds! []
  (reset! birds (remove out-of-bounds? @birds)))

;; Int -> []
;; Update the state of the program by:
;;   1. moving the current birds
;;   2. removing birds that have gone out of bounds
;;   3. adding new birds to the program
;;   4. resetting the timestamp
;; After updating the state it will call for the next update
(defn update! [ts]
  (next-birds! (- ts (or @last-ts ts)))
  (remove-out-of-bounds!)
  (add-birds!)
  (reset! last-ts ts)
  (. js/window (requestAnimationFrame update!)))

;; [] -> []
;; Shows the loading box until the images are loaded
;; after the images are done loading:
;;   1. show the background image
;;   2. create the initial batch of birds
;;   3. render the normal ui
;;   4. start the animation
(defn load-images []
  (if (and (:bgloaded @options) (:loaded @bird-sprite))
    (do (add-birds!)
        (r/render [catnip-ui] (.getElementById js/document "app"))
        (. js/window (requestAnimationFrame update!)))
    (js/setTimeout load-images 16)))

;; -------------------------
;; Initialize app

;; [] -> []
;; Sets up the initial state of the program and then kicks off the animation loop
(defn mount-root []
  (do
    (r/render [splash-ui] (.getElementById js/document "app"))
    (load-images)))



;; [] -> []
;; This is inistial entry point when called by the web browser and just calls mount-root
(defn init! []
  (mount-root))