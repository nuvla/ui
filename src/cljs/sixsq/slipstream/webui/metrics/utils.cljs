(ns sixsq.slipstream.webui.metrics.utils
  (:require [sixsq.slipstream.client.api.cimi :as cimi]
            [sixsq.slipstream.webui.cimi-api.utils :as cimi-api-utils]))

(def default-params {:$first 1, :$last 0})

(def old-jobs (assoc default-params :$filter "created < 'now-8d'"))

(def stale-jobs (assoc default-params :$filter "created < 'now-30m' and state = 'QUEUED'"))

(def blocked-jobs (assoc default-params :$filter "updated < 'now-15m' and state = 'RUNNING'"))

(def job-states (assoc default-params :$aggregation "terms:state"))

(defn job-search
  [client params]
  (cimi/search client "jobs" (cimi-api-utils/sanitize-params params)))

(defn add-total
  [buckets]
  (let [total (->> buckets
                   (map :doc_count)
                   (reduce +))]
    (conj buckets {:key "TOTAL", :doc_count total})))

(defn extract-thread-metrics
  [{{terminated :value}    :jvm.thread.terminated.count
    {runnable :value}      :jvm.thread.runnable.count
    {total :value}         :jvm.thread.count
    {deadlocked :value}    :jvm.thread.deadlock.count
    {new :value}           :jvm.thread.new.count
    {daemon :value}        :jvm.thread.daemon.count
    {timed-waiting :value} :jvm.thread.timed_waiting.count
    {waiting :value}       :jvm.thread.waiting.count
    {blocked :value}       :jvm.thread.blocked.count
    :as                    metrics}]
  (when metrics
    [{:state "terminated", :threads terminated}
     {:state "runnable", :threads runnable}
     {:state "total", :threads total}
     {:state "deadlocked", :threads deadlocked}
     {:state "new", :threads new}
     {:state "daemon", :threads daemon}
     {:state "timed-waiting", :threads timed-waiting}
     {:state "waiting", :threads waiting}
     {:state "blocked", :threads blocked}]))


(defn extract-memory-metrics
  [{{non-heap-committed :value} :jvm.memory.non-heap.committed
    {non-heap-init :value}      :jvm.memory.non-heap.init
    {non-heap-used :value}      :jvm.memory.non-heap.used
    {non-heap-max :value}       :jvm.memory.non-heap.max

    {heap-committed :value}     :jvm.memory.heap.committed
    {heap-used :value}          :jvm.memory.heap.used
    {heap-init :value}          :jvm.memory.heap.init
    {heap-max :value}           :jvm.memory.heap.max

    {total-committed :value}    :jvm.memory.total.committed
    {total-used :value}         :jvm.memory.total.used
    {total-init :value}         :jvm.memory.total.init
    {total-max :value}          :jvm.memory.total.max
    :as                         metrics}]
  (when metrics
    (filter #(not (neg? (:memory %)))
            [{:type "non-heap committed", :memory non-heap-committed}
             {:type "non-heap init", :memory non-heap-init}
             {:type "non-heap used", :memory non-heap-used}
             {:type "non-heap max", :memory non-heap-max}

             {:type "heap committed", :memory heap-committed}
             {:type "heap init", :memory heap-init}
             {:type "heap used", :memory heap-used}
             {:type "heap max", :memory heap-max}

             {:type "total committed", :memory total-committed}
             {:type "total init", :memory total-init}
             {:type "total used", :memory total-used}
             {:type "total max", :memory total-max}])))


(defn get-rate
  "extracts the 1-minute average rate for the method"
  [metrics type k]
  (when metrics
    (let [section-kw (keyword (str "ring." type ".rate." k))
          path [section-kw :rates :1]
          rate (get-in metrics path)]
      {(keyword type) (keyword k), :rate rate})))


(defn extract-ring-requests-rates
  [metrics]
  (let [methods ["GET" "POST" "PUT" "DELETE"]]
    (mapv (partial get-rate metrics "requests") methods)))


(defn extract-ring-responses-rates
  [metrics]
  (let [status-codes ["2xx" "3xx" "4xx" "5xx"]]
    (mapv (partial get-rate metrics "responses") status-codes)))
