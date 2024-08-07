;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) KALEIDOS INC

(ns app.util.text.content.styles
  (:require
   [app.common.transit :as transit]))

(defn encode
  [value]
  (transit/encode-str value))

(defn decode
  [value]
  (if (= value "")
    nil
    (transit/decode-str value)))

(def mapping
  {:fills ["--fills" encode decode]
   :typography-ref-id ["--typography-ref-id" encode decode]
   :typography-ref-file ["--typography-ref-file" encode decode]
   :font-id ["--font-id" identity identity]
   :font-variant-id ["--font-variant-id" identity identity]})

(defn needs-mapping?
  [key]
  (let [contained? (contains? mapping key)]
    contained?))

(defn map-style-key
  [key]
  (if (needs-mapping? key)
    (let [[name] (get mapping key)]
      (keyword name))
    key))

(defn map-style-value
  [key value]
  (if (needs-mapping? key)
    (let [[_ encoder] (get mapping key)]
      (encoder value))
    value))

(defn map-style
  [[key value]]
  [(map-style-key key)
   (map-style-value key value)])

(defn map-styles
  [styles]
  (let [mapped-styles
        (into {} (map map-style styles))]
    (clj->js mapped-styles)))
