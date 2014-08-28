(ns om-drag-drop.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :as async
             :refer [>! <! put! chan alts!]]))

(enable-console-print!)

(def app-state (atom [{:id 1 :text "one"}
                      {:id 2 :text "two"}
                      {:id 3 :text "three"}
                      {:id 4 :text "four"}
                      {:id 5 :text "five"}
                      {:id 6 :text "six"}]))

(defn- get-id [dom-element]
  (-> dom-element
      .-dataset
      (aget "reactid")
      (.split "$")
      second
      int))

(om/root
  (fn [app owner]
    (reify
      om/IInitState
      (init-state [_]
        {:dragging false
         :dragTarget nil
         :dropTarget nil
         :dragChannel (chan)})
      om/IWillMount
      (will-mount [_]
        (let [channel (om/get-state owner :dragChannel)]
          (go (loop []
                (let [element (<! channel)]
                  (om/set-state! owner :dropTarget (get-id element))
                  (recur))))))
      om/IRenderState
      (render-state [this state]
        (dom/div nil
                 (dom/h1 nil (str "Dragging? " (:dragging state) " " (:dragTarget state)))
                 (apply dom/ul #js {:onMouseDown (fn [e]
                                                   (om/set-state! owner :dragging true)
                                                   (om/set-state! owner :dragTarget (get-id (.-target e))))
                                    :onMouseUp (fn []
                                                 (om/set-state! owner :dragging false)
                                                 (om/set-state! owner :dragTarget nil)
                                                 (om/set-state! owner :dropTarget nil))
                                    :onMouseMove (fn [e]
                                                   (when (:dragging state)
                                                     (put! (:dragChannel state) (.-target e))))}
                        (map (fn [item] (dom/li #js {:key (:id item)} (if (= (:id item)
                                                                             (:dropTarget state))
                                                                        (str "Drop on me!")
                                                                        (:text item))))
                             app))))))
  app-state
  {:target (. js/document (getElementById "app"))})
