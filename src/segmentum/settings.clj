(ns segmentum.settings
  (:refer-clojure :exclude [get set])
  (:require [segmentum.db.core :as db]
            [mount.core :refer [defstate]]
            [clojure.data.json :as json]))


;;TODO add schema
(defonce registered-settings (atom {}))
(defonce settings-vals (atom {}))


(defstate settings-from-db
  :start (swap! settings-vals merge
           (->> (db/query :get-settings)
             (map #(vector (keyword (:key %)) (:value %)))
             (into {}))))


(defn- register-setting! [opts]
  (let [defaults {:default       nil
                  :type          :string
                  :cache?        true
                  :confidential? false}
        opts     (merge defaults opts)]
    (swap! registered-settings assoc (:name opts) opts)
    opts))


(defn- type-casting [type v]
  (case type
    :bool (Boolean/parseBoolean v)
    :int (Integer/parseInt v)
    :double (Double/parseDouble v)
    :json (json/read-str v :key-fn keyword)
    v))


(defn create-setting-fn [setting]
  (fn
    ([]
     (if-let [s (@registered-settings (:name setting))]
       (when-let [v (or (when (:cache? s) (@settings-vals (:name setting)))
                      (db/query :get-setting {:key (-> setting :name name)})
                      (:default s))]
         (type-casting (:type s) v))
       (throw (IllegalArgumentException. "There is no such a setting registered: " (:name setting)))))
    ([new-val]
     (swap! settings-vals assoc (:name setting) new-val)
     (db/query :set-setting! {:key (-> setting :name name) :value new-val}))))


(defmacro defsetting
  "Creates a new setting that will be added to the database.

   Usage: (smtp-host) => returns smtp host's value
          (smtp-host \"http://smtp.gmail.com\") => updates SMTP host with given value

   You can pass options too:
   :default       - The default value of the setting. (default: nil)
   :type          - :string (default), :bool, :int, :json, :double
   :cache?        - Should this setting be cached? (default true) - Disabling this not recommended.
   :confidential? - Is this a confidential setting, such as a password etc. (default false)"
  [name desc & {:as opts}]
  {:pre [(symbol? name)]}
  `(let [setting# (register-setting! (assoc ~opts
                                       :name ~(keyword name)
                                       :description ~desc))]
     (def ~name (create-setting-fn setting#))))


(defn get
  "Gets setting value by given key.
   Usage: (get :timeout :int)"
  ([key]
   (get key :string))
  ([key type]
   (let [v (or (@settings-vals key)
             (:value (db/query :get-setting {:key (name key)})))]
     (when-not v (throw (Exception. (str "Setting not found: " key))))
     (type-casting type v))))


(defn set
  "Sets settings by given key and value.
   Usage: (set :smtp-host \"http://myhost.com\")"
  [key val]
  (swap! settings-vals assoc key (str val))
  (db/query :set-setting! {:key (name key) :value (str val)}))