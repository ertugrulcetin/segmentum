(ns segmentum.util.imports
  (:require [segmentum.api.common]
            [potemkin :as p]))


(p/import-vars
  [segmentum.api.common

   ->ModelValidationException
   resource])