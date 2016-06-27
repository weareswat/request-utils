# Request Utils [![Build Status](https://travis-ci.org/weareswat/request-utils.svg?branch=master)](https://travis-ci.org/weareswat/request-utils)

[![Clojars Project](https://clojars.org/weareswat/request-utils/latest-version.svg)](https://clojars.org/weareswat/request-utils)

A Clojure utility to do async http requests with Aleph. This library simplifies all the related boilerplate.

Installation
-----

```request-utils``` is available as a Maven artifact from [Clojars](https://clojars.org/weareswat/request-utils)

With Leiningen/Boot:

```clojure
[weareswat/request-utils "0.4.0"]
```

### Operations

For now we just support the following operations:
* `get` via `http-get` fn
* `post` via `http-post` fn
* `put` via `http-put` fn

Every operation has an async interface, so it returns a channel.

* `GET to http://app.clanhr.com/directory-api/`

```clojure

(ns my-app.core
  (:require [request-utils.core :as request-utils]))

(defn example-of-request
  []
  (request-utils/http-get {:host "http://app.clanhr.com/directory-api/"}))
 
```

The `request-utils/http-get` should return a map in the following format:

```
	{:success true
 	 :status 200
 	 :requests 1
 	 :name ClanHR Directory API} 
```

As you can see in the following example the data is a map with all data related with the request:

```
   {:host 'http://api.fake.com'
    :path '/fake/path'
    :query-params {:a '1' :b '2'}
    :headers {'content-type' 'application/json'
              'Authorization' 'ewqjwehqkwh'}
    :body {:name 'fake name' :age 34}}
```

Only the host is required. It's mandatory to have the protocol and the host together.
  * Valid :host value: http://api.fake.com
  * Invalid :host value: api.fake.com

#### Tests

* `lein test` - runs the test suite *
* `lein autotest` -listen for file changes and is always running tests

#### Hey, did you found an issue?

The best way to get in touch is using the [GitHub issues section](https://github.com/weareswat/request-utils/issues).  
If you can't find someone with the problem you are facing open a [new issue](https://github.com/weareswat/request-utils/issues/new) and let us know.  
If you manage to find a solution for your problem, you can submit a new [PR](https://github.com/weareswat/request-utils/pulls) :)

Let's make the world a better place by helping others.

# License
[MIT](https://github.com/weareswat/request-utils/blob/master/LICENSE)
