(ns catnip.core
  (:require [reagent.core :as r :refer [atom]]
            [reagent.session :as session]
            [secretary.core :as secretary :include-macros true]
            [catnip.fullscreen :as fullscreen]
            [catnip.sprite :as sprite]))

;; -------------------------
;; State information

;; options is:
;; {:max Int :speed Int :showui Bool :bgimg String :bgloaded Bool :running Bool}
;;    - :max      is the maxiumum number of birds that can be on screen at a time
;;    - :speed    is a scaling factor for the spped the birds fly
;;    - :showui   is a flag for determining if the UI elements should be shown or not
;;    - :bgimg    is the path to the background image
;;    - :bgloaded lets the program know if the browser has finished loading the bgimg yet
;;    - :running  lets the program know if the animation should be running or not
(defonce options (r/atom {:max      2 :speed 3 :showui true
                          :bgimg    "bg/nature-forest-waterfall-jungle.jpg"
                          :bgloaded false :running false}))

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

;; bird-sheet is:
;; {:img String :width Int :height Int :frames Int :loaded Bool}
;; interp. a sprite sheet
;;    - :img    is the path to the the sprite sheet
;;    - :width  is the width of the sprite
;;    - :height is the height of the sprite
;;    - :frames is the number of fames in the sprite sheet
;;    - :loaded lets the program know if the img is loaded yet
(defonce bird-sheet (r/atom {:img   "sprite/blue-jay-sprite-sheet.png"
                             :width 100 :height 100 :cols 8 :rows 1 :loaded false}))

;; bird-sprites is:
;; {:left [Component] :right [Component]}
;; interp. the img components that show each frame of animation
;;     - :left  is a vector containing all the left facing frames
;;     - :right is a vector containing all the right facing frames
(defonce bird-sprites (r/atom {}))

;; -------------------------
;; Functions

;; Int -> []
;; pauses the program for the given number of milliseconds, useful for testing
(defn sleep [msec]
  (let [deadline (+ msec (.getTime (js/Date.)))]
    (while (> deadline (.getTime (js/Date.))))))

;; [] -> {:top Int :left Int :bottom Int :right Int}
;; produce a map of the bounds of the screen area with padding for the size of the bird sprite
(defn get-bounds []
  (let [width (.-innerWidth js/window)
        height (.-innerHeight js/window)]
    {:top (- (:height @bird-sheet)) :left (- (:width @bird-sheet)) :bottom height :right width}))

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

;; Int Bird -> Bird
;; produce a new bird with a new position and frame number based on the old bird and the delta of time since the last
;; update
(defn update-bird [delta bird]
  (let [speed (/ (* delta (:speed @options)) 20)]
    {:x     (+ (:x bird) (* speed (:dx bird)))
     :y     (+ (:y bird) (* speed (:dy bird)))
     :dx    (:dx bird)
     :dy    (:dy bird)
     :frame (rem (+ (/ delta 60) (:frame bird)) (count (:left @bird-sprites)))
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
        y (- (rand-int (:bottom bounds)) (:height @bird-sheet))
        f (rand-int (count (:left @bird-sprites)))]
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
  (if (:running @options)
    (do (next-birds! (- ts (or @last-ts ts)))
        (remove-out-of-bounds!)
        (add-birds!)
        (reset! last-ts ts)
        (. js/window (requestAnimationFrame update!)))))

;; -------------------------
;; Reagent components

;; [] -> Component
;; produce the loading box at the center of the screen
(defn splash-ui []
  [:div
   [:div.splash.ui
    [:h3 "Catnip"]
    [:div {:style {:text-align "center"
                   :font-size  "x-large"}}
     "Loading " [:i.fa.fa-spinner.fa-spin]
     [:img {:src   (:img @bird-sheet)
            :style {:display "none"}
            :load  (swap! bird-sheet assoc :loaded true)}]
     [:img {:src   (:bgimg @options)
            :style {:display "none"}
            :load  (swap! options assoc :bgloaded true)}]]]])

;; Bird -> Component
;; produce a div positioned at the bird's x and y co-ordinates. Inside the div is an img showing the current frame
;; of the bird sprite. The :key from the bird is put in the meta data of the div so that Reagent can tell which bird
;; is which.

(defn bird-component [bird]
  (let [rotate (if (neg? (:dx bird))
                 (* 3 (:dy bird))
                 (* -3 (:dy bird)))
        facing (if (neg? (:dx bird))
                 :left
                 :right)]
    ^{:key {:key bird}}
    [:div {:style {:position  "fixed"
                   ;; Uncomment border to see the bounding box for each bird
                   ;:border    "2px solid red"
                   :width     (:width @bird-sheet)
                   :height    (:height @bird-sheet)
                   :top       (:y bird)
                   :left      (:x bird)
                   :transform (str "rotate(" rotate "deg)")}}
     (get (facing @bird-sprites) (:frame bird))]))

;; Atom Key Int Int -> Component
;; produce a slider style input that is at the current value of the atom's param and can range between min and max
;; if the user adjusts the slider the atom's param will be updated to match the current value of the slider
(defn slider [atom param min max]
  [:input {:type      "range" :value (param (deref atom)) :min min :max max
           :style     {:width "90%"}
           :on-change (fn [e]
                        (swap! atom assoc param (.-target.value e)))}])

;; [] -> Component
;; produce the options button
(defn options-button []
  [:div.tooltip.toolbarbutton
   [:i.fa.fa-cogs
    {:on-click #(swap! options assoc :showui (not (:showui @options)))}]
   [:span.tooltiptext.left
    (if (:showui @options)
      "Hide options"
      "Show options")
    ]])

;; [] -> Component
;; produce the fullscreen button
(defn fullscreen-button []
  [:div.tooltip.toolbarbutton
   [:i.fa.fa-arrows-alt
    {:on-click (fn [e]
                 (.preventDefault e)
                 (fullscreen/toggle (.getElementById js/document "app")))}]
   [:span.tooltiptext.right
    (if (fullscreen/is-fullscreen?)
      "Exit fullscreen"
      "Fullscreen")
    ]])

;; [] -> Component
;; produce a button the will pause or unpause the animation
(defn play-pause-button []
  [:div.tooltip.toolbarbutton
   (if (:running @options)
     [:i.fa.fa-pause.fa-fw
      {:on-click (fn [e]
                   (.preventDefault e)
                   (swap! options assoc :running false))}]
     [:i.fa.fa-play.fa-fw
      {:on-click (fn [e]
                   (.preventDefault e)
                   (swap! options assoc :running true)
                   (. js/window (requestAnimationFrame update!)))}])
   [:span.tooltiptext.left
    (if (:running @options)
      "Pause"
      "Play")]])

;; [] -> Component
;; produce a button that will start or stop the animation
(defn start-stop-button []
  [:button.ui {:style {:padding   "5px"
                       :width     "90px"
                       :font-size "large"}}
   (if (:running @options)
     [:span
      {:on-click (fn [e]
                   (.preventDefault e)
                   (swap! options assoc :running false)
                   (reset! birds []))}
      [:i.fa.fa-stop] " Stop"]
     [:span
      {:on-click (fn [e]
                   (.preventDefault e)
                   (swap! options assoc :showui false)
                   (swap! options assoc :running true)
                   (. js/window (requestAnimationFrame update!)))}
      [:i.fa.fa-play] " Start"])])

;; [] -> Component
(defn close-button []
  [:i.fa.fa-times {:on-click (fn [e]
                               (.preventDefault e)
                               (swap! options assoc :showui false))
                   :style    {:visibility (if (and (:running @options)
                                                   (:showui @options))
                                            "visible"
                                            "hidden")
                              :font-size  "large"}}])

;; [] -> Component
;; produce the UI for the program
(defn catnip-ui []
  [:div.bg {:style {:backgroundImage (str "url(\"" (:bgimg @options) "\")")}}
   [:div (doall (map bird-component @birds))]
   [:div.toolbar
    (play-pause-button)
    (options-button)
    [:span {:style {:position "absolute"
                    :right    0}} (fullscreen-button)]]
   [:table.options.ui {
                       :style {:visibility (if (:showui @options)
                                             "visible"
                                             "hidden")
                               :opacity    (if (:showui @options)
                                             1
                                             0)}}
    [:tr
     [:td {:col-span 3}
      [:i.fa.fa-cogs] " Options"
      [:span {:style {:position "absolute"
                      :right    "3px"
                      :top      "1px"}}
       (close-button)]]]
    [:tr
     [:td "Birds: "]
     [:td [slider options :max 1 10]]
     [:td (:max @options)]]
    [:tr
     [:td [:div "Speed: "]
      [:td [slider options :speed 1 10]]
      [:td (:speed @options)]]]
    [:tr
     [:td {:col-span 3
           :style    {:text-align "center"}}
      (start-stop-button)]]]])

;; -------------------------
;; Initialize app

;; [] -> []
;; Shows the loading box until the images are loaded
;; after the images are done loading:
;;   1. show the background image
;;   2. create the initial batch of birds
;;   3. render the normal ui
;;   4. start the animation
(defn load-images []
  (if (and (:bgloaded @options) (:loaded @bird-sheet))
    (r/render [catnip-ui] (.getElementById js/document "app"))
    (js/setTimeout load-images 16)))

;; [] -> []
;; Sets up the initial state of the program and then kicks off the animation loop
(defn mount-root []
  (do
    (r/render [splash-ui] (.getElementById js/document "app"))
    (swap! bird-sprites assoc :left (sprite/get-sprites (:img @bird-sheet)
                                                          (:width @bird-sheet)
                                                          (:height @bird-sheet)
                                                          (:cols @bird-sheet)
                                                          (:rows @bird-sheet)
                                                          :horz))
    (swap! bird-sprites assoc :right (sprite/get-sprites (:img @bird-sheet)
                                                        (:width @bird-sheet)
                                                        (:height @bird-sheet)
                                                        (:cols @bird-sheet)
                                                        (:rows @bird-sheet)
                                                        :none))
    (load-images)))



;; [] -> []
;; This is inistial entry point when called by the web browser and just calls mount-root
(defn init! []
  (mount-root))