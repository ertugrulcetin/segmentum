(ns segmentum.imports
  (:require [segmentum.api.util]
            [potemkin :as p]))


(p/import-vars
  [segmentum.api.util
   ->ModelValidationException
   resource])