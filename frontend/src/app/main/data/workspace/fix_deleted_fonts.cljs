;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) KALEIDOS INC

(ns app.main.data.workspace.fix-deleted-fonts
  (:require
   [app.common.data :as d]
   [app.common.files.helpers :as cfh]
   [app.common.text :as txt]
   [app.main.data.changes :as dwc]
   [app.main.data.workspace.state-helpers :as wsh]
   [app.main.fonts :as fonts]
   [beicon.v2.core :as rx]
   [potok.v2.core :as ptk]))

;; This event will update the file so the texts with non existing custom fonts try to be fixed.
;; This can happen when:
;; - Exporting/importing files to different teams or penpot instances
;; - Moving files from one team to another in the same instance
;; - Custom fonts are explicitly deleted in the team area

(defn calculate-alternative-font-id
  [value]
  (let [fonts (deref fonts/fontsdb)]
    (->> (vals fonts)
         (filter #(= (:family %) value))
         (first)
         :id)))

(defn has-invalid-font-family
  [node]
  (let [fonts               (deref fonts/fontsdb)
        font-family         (:font-family node)
        alternative-font-id (calculate-alternative-font-id font-family)]
    (and
     (some? font-family)
     (nil? (get fonts (:font-id node)))
     (some? alternative-font-id))))

(defn should-fix-deleted-font-shape?
  [shape]
  (let [text-nodes (txt/node-seq txt/is-text-node? (:content shape))]
    (and (cfh/text-shape? shape) (some has-invalid-font-family text-nodes))))

(defn should-fix-deleted-font-component?
  [component]
  (->> (:objects component)
       (vals)
       (d/seek should-fix-deleted-font-shape?)))

(defn fix-deleted-font
  [node]
  (let [alternative-font-id (calculate-alternative-font-id (:font-family node))]
    (cond-> node
      (some? alternative-font-id) (assoc :font-id alternative-font-id))))

(defn fix-deleted-font-shape
  [shape]
  (let [transform (partial txt/transform-nodes has-invalid-font-family fix-deleted-font)]
    (update shape :content transform)))

(defn fix-deleted-font-component
  [component]
  (update component
          :objects
          (fn [objects]
            (d/mapm #(fix-deleted-font-shape %2) objects))))

(defn fix-deleted-font-typography
  [typography]
  (let [alternative-font-id (calculate-alternative-font-id (:font-family typography))]
    (cond-> typography
      (some? alternative-font-id) (assoc :font-id alternative-font-id))))

(defn- generate-deleted-font-shape-changes
  [{:keys [objects id]}]
  (let [shapes (into #{}
                     (comp (filter should-fix-deleted-font-shape?))
                     (vals objects))]
    (into []
          (map (fn [shape]
                 {:type :mod-obj
                  :id (:id shape)
                  :page-id id
                  :operations [{:type :set
                                :attr :content
                                :val (:content (fix-deleted-font-shape shape))}
                               {:type :set
                                :attr :position-data
                                :val nil}]}))
          shapes)))

(defn- generate-deleted-font-components-changes
  [state]
  (let [components (->> (wsh/lookup-local-components state)
                        (vals)
                        (filter should-fix-deleted-font-component?))]
    (into []
          (map (fn [component]
                 {:type :mod-component
                  :id (:id component)
                  :objects (-> (fix-deleted-font-component component) :objects)}))
          components)))

(defn- generate-deleted-font-typography-changes
  [state]
  (let [typographies (->> (get-in state [:workspace-data :typographies])
                          (vals)
                          (filter has-invalid-font-family))]
    (into []
          (map (fn [typography]
                 {:type :mod-typography
                  :typography (fix-deleted-font-typography typography)}))
          typographies)))

(defn fix-deleted-fonts
  []
  (ptk/reify ::fix-deleted-fonts
    ptk/WatchEvent
    (watch [it state _]
      (let [data               (get state :workspace-data)
            shape-changes      (mapcat generate-deleted-font-shape-changes (vals (:pages-index data)))
            components-changes (generate-deleted-font-components-changes state)
            typography-changes (generate-deleted-font-typography-changes state)
            changes (concat shape-changes components-changes typography-changes)]
        (if (seq changes)
          (rx/of (dwc/commit-changes
                  {:origin it
                   :redo-changes (vec changes)
                   :undo-changes []
                   :save-undo? false}))
          (rx/empty))))))
