(ns lobsang.core
  (:require [clojure.java.io :as io]
            [clj-http.client :as http]
            [lobsang.zip :as zip])
  (:import (net.fortuna.ical4j.data CalendarParserFactory
                                    CalendarBuilder)
           (net.fortuna.ical4j.model Date
                                     DateTime
                                     Dur
                                     Period
                                     TimeZoneRegistryFactory)
           (net.fortuna.ical4j.filter PeriodRule
                                      Rule
                                      Filter)
           (java.util Properties)
           (java.util.concurrent Executors
                                 ScheduledThreadPoolExecutor
                                 TimeUnit)
           (java.util GregorianCalendar
                      Calendar
                      TimeZone)))

(def time-zone-registry-factory (TimeZoneRegistryFactory/getInstance))

(def time-zone-registry (.createRegistry time-zone-registry-factory))

(def utc (.getTimeZone time-zone-registry "UTC"))

(defn calendar [thing]
  (try
    (let [parser-factory (CalendarParserFactory/getInstance)
          parser (.createParser parser-factory)
          builder (CalendarBuilder. parser)]
      (.build builder (io/input-stream thing)))
    (catch Exception _)))

(defn events [calendar]
  (.getComponents calendar "VEVENT"))

(defn minute-period [minutes tz]
  (doto (Period. (DateTime.)
                 (Dur. 0 0 minutes 0))
    (.setTimeZone (.getTimeZone time-zone-registry tz))))

(defn find-in [calendar period]
  (let [rules (make-array Rule 1)]
    (aset rules 0 (PeriodRule. period))
    (let [filter-thing (Filter. rules Filter/MATCH_ALL)]
      (.filter filter-thing (events calendar)))))

(def run-events (atom #{}))

(defonce scheduled-executor (Executors/newScheduledThreadPool 3))

(defn daily-event? [event]
  (let [dur (Dur.
             (.getDate (.getStartDate event))
             (.getDate (.getEndDate event)))]
    (and (= 1 (.getDays dur))
         (zero? (.getHours dur))
         (zero? (.getMinutes dur))
         (zero? (.getSeconds dur)))))

(defn event-length [event]
  (let [start (.getDate (.getStartDate event))
        end (.getDate (.getEndDate event))
        now (java.util.Date.)
        event-ends-by (if (daily-event? event)
                        (let [cal (doto (GregorianCalendar.
                                         (TimeZone/getTimeZone "utc"))
                                    (.setTime now)
                                    (.set Calendar/HOUR_OF_DAY 0)
                                    (.set Calendar/MINUTE 0)
                                    (.set Calendar/SECOND 0)
                                    (.set Calendar/MILLISECOND 0))]
                          (.set cal Calendar/DAY_OF_YEAR
                                (inc (.get cal Calendar/DAY_OF_YEAR)))
                          (.getTime cal))
                        (.getTime (Dur. start end) now))]
    (- (.getTime event-ends-by) (.getTime now))))

(defn calendar-timezone [calendar]
  (try
    (-> (.getComponents calendar "VTIMEZONE")
        first
        (.getProperties "TZID")
        first
        .getValue)
    (catch Exception _
      "UTC")))

(defn execute-events [calendar executor]
  (let [tz (calendar-timezone calendar)]
    (doseq [event (find-in calendar (minute-period 1 tz))
            :let [id (.getValue (.getUid event))
                  p (Properties.)]
            :when (not (contains? @run-events id))]
      (.load p (io/input-stream (.getBytes (.getValue (.getDescription event)))))
      (when (get p "URL")
        (http/get (get p "URL")
                  ;; Properties are maps
                  {:query-params p}))
      (swap! run-events conj id)
      (.schedule executor
                 ^Callable (fn [] (swap! run-events disj id))
                 (event-length event)
                 TimeUnit/MILLISECONDS))))

(def calendars (atom {}))

(defn run-calendar [is]
  (execute-events (calendar is) scheduled-executor))

(defonce run-calendar-events
  (.scheduleAtFixedRate
   scheduled-executor
   (bound-fn []
     (doseq [[calender bytes] @calendars]
       (future (run-calendar (io/input-stream (zip/unzip-bytes bytes))))))
   0
   1
   TimeUnit/MINUTES))

(def calendar-polls (atom {}))

(defn register-calendar [url minutes]
  (let [fut (.scheduleAtFixedRate
             scheduled-executor
             (bound-fn []
               (let [{:keys [body]} (http/get url {:insecure? true})]
                 (swap! calendars assoc url
                        (zip/zip-bytes (.getBytes body)))))
             0
             minutes
             TimeUnit/MINUTES)]
    (swap! calendar-polls (fn [polls]
                            (when (contains? polls url)
                              (.cancel (get polls url) true))
                            (assoc polls url fut)))
    nil))
