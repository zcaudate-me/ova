# ova

stateful arrays for clojure

[![Build Status](https://travis-ci.org/zcaudate/ova.png?branch=master)](https://travis-ci.org/zcaudate/ova)

## DEPRECATION NOTICE

[ova](https://github.com/zcaudate/ova) has been merged into [hara](https://github.com/zcaudate/hara). Please see [updated docs](http://docs.caudate.me/hara/hara-concurrent-ova.html) for updated version.

### Installation

Add to `project.clj`

    [im.chit/ova "1.0.1"]

All functions are in the `ova.core` namespace. 

### Documentation

See main site at:

http://docs.caudate.me/ova/

To generate this document for offline use: 

  1. Clone this repository
  
    > git clone https://github.com/zcaudate/ova.git
  
  2. Install [lein-midje-doc](http://docs.caudate.me/lein-midje-doc). 
  
  3. Create `doc` folder
      
    > mkdir doc

  4. Run in project folder
  
    > lein midje-doc

The output will be generated in `doc/index.html`

### Blog

See [You took 3 months to write a mutable array for clojure?](http://z.caudate.me/you-took-3-months-to-write-a-mutable-array/) for motivation and write up.


## License
Copyright © 2013 Chris Zheng

Distributed under the MIT License
