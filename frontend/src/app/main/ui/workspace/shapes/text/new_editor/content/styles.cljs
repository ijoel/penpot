;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) KALEIDOS INC

(ns app.main.ui.workspace.shapes.text.new-editor.content.styles
  (:require
   [app.common.text :as txt]
   [app.common.transit :as transit]))

(defn encode
  [value]
  (.stringify js/JSON value))

(defn decode
  [value]
  (if (= value "")
    nil
    (.parse js/JSON value)))

(def mapping
  {:fills ["--fills" encode decode]
   :typography-ref-id ["--typography-ref-id" encode decode]
   :typography-ref-file ["--typography-ref-file" encode decode]
   :font-id ["--font-id" encode decode]
   :font-variant-id ["--font-variant-id" encode decode]})


