(ns lobsang.web
  (:require [ring.middleware.multipart-params :refer [wrap-multipart-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.params :refer [wrap-params]]))

(def register-calendar
  (delay @(resolve 'lobsang.core/register-calendar)))

(defn init []
  (require 'lobsang.core))

(defn calendar-loader [{{:keys [calendar minutes]} :params}]
  (@register-calendar calendar (Long/parseLong minutes))
  {:status 200
   :body "hello world"})

(def handler
  (-> #'calendar-loader
      wrap-keyword-params
      wrap-multipart-params
      wrap-params))
