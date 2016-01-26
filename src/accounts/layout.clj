(ns accounts.layout
  (:require [hiccup.core :refer :all]
            [hiccup.page :refer [html5 include-css]]))

(defn base
  "Base HTML layout"
  [content & [{title :title :or {title "Accounts"}}]]
  (html5
   (list [:meta {:charset :utf-8}]
         [:meta {:name :viewport :content "width=device-width, initial-scale=1.0"}]
         [:title (h title)]
         [:meta {:name :author :content "Stig Brautaset"}]
         (include-css "//cmcenroe.me/writ/writ.min.css")
         content)))
