(ns catnip.sprite)

;; Natural Natural Natural Natural -> ({:left Natural :right Natural :top Natural :bottom Natural})
;; produce a list of maps that contain the left, right, top, and bottom positions of each sprite
(defn get-rects [imgWidth imgHeight spriteWidth spriteHeight]
  (let [leftsAndRights (map (fn [x] {:left x :right (+ x spriteWidth)}) (range 0 imgWidth spriteWidth))
        topsAndBottoms (mapv (fn [y] {:top y :bottom (+ y spriteHeight)}) (range 0 imgHeight spriteHeight))]
    (flatten (map (fn [tb] (map #(merge tb %) leftsAndRights)) topsAndBottoms))))

;; {:left Natural :right Natural :top Natural :bottom Natural} Natural Natural Key -> {:scaleX Int :scaleY Int
;;                                                                                     :transX Int :transY Ingeger}
;; produce the scaling and translation parameters for the matrix transform to show the correct part of the
;; sprite's source image
(defn get-matrix [rect imgWidth imgHeight flip]
  (case flip
    :none {:scaleX 1 :scaleY 1 :transX (- (:left rect)) :transY (- (:top rect))}
    :horz {:scaleX -1 :scaleY 1 :transX (- (:right rect) imgWidth) :transY (- (:top rect))}
    :vert {:scaleX 1 :scaleY -1 :transX (- (:left rect)) :transY (- (:bottom rect) imgHeight)}
    :both {:scaleX -1 :scaleY -1 :transX (- (:right rect) imgWidth) :transY (- (:bottom rect) imgHeight)}))

;; ({:left Natural :right Natural :top Natural :bottom Natural}) Natural Natural Key -> ({:left Natural :right Natural
;;                                                                                        :top Natural :bottom Natural
;;                                                                                        :scaleX Int :scaleY Int
;;                                                                                        :transX Int :transY Int})
(defn get-rects-with-matrix [rects imgWidth imgHeight flip]
  (map (fn [r] (merge r (get-matrix r imgWidth imgHeight flip))) rects))

;; {:left Natural :right Natural :top Natural :bottom Natural} -> String
;; produce the HTML rect property string in the form "rect(top, right, bottom, left)"
(defn get-rect-string [r]
  (str "rect(" (:top r) "px, " (:right r) "px, " (:bottom r) "px, " (:left r) "px)"))

;; {:scaleX Int :scaleY Int :transX Int :transY Int} -> String
;; produce the HTML matrix transform propertey stiring in the form
;;                                                       "matrix(scaleX, skewY, skewX, scaleY, translateX, translateY)"
;; Sprites don't need to be skewed so those falues are left at zero
(defn get-matrix-string [m]
  (str "matrix(" (:scaleX m) ", 0, 0, " (:scaleY m) ", " (:transX m) ", " (:transY m) ")"))

;; rectWMatrix String -> [:img]
;; produce a DOM img tag component for the sprite
(defn get-img [rwm src]
  [:img {:style {:position  "absolute"
                 :clip      (get-rect-string rwm)
                 :transform (get-matrix-string rwm)}
         :src   src}])

;; String Natural Natural Natural Natural Key -> [[:img]]
;; produce a vector of img DOM components containing each frame of the sprite
;; flip can be:
;;      :none - don't flip
;;      :horz - flip horizontally
;;      :vert - flip vertically
;;      :both - flip on both axis
(defn get-sprites [src spriteWidth spriteHeight cols rows flip]
  (let [imgWidth (* spriteWidth cols)
        imgHeight (* spriteHeight rows)
        rects (get-rects imgWidth imgHeight spriteWidth spriteHeight)
        rwm (get-rects-with-matrix rects imgWidth imgHeight flip)]
    (mapv (fn [r] (get-img r src)) rwm)))