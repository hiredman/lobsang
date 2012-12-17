# lobsang

Lobsang is the son of Wen the Eternally Surprised

## Usage

create a uberwar

    lein uberwar

deploy war to your prefered container.

send urls for ical calendars via rest api.
this example will refresh the calendar from the given url every 10 minutes

    curl --data "calendar=http://example.com/cal.ics&minutes=10" localhost:8081

watch things happen.

also possible to use as a library.

for events (VEVENTS) with the a descitpion like:

    URL=http://example.com/foo
    A=1
    B=2

it will do a get request like 
    
    http://example.com/foo?A=1&B=2

## License

Copyright © 2012 Kevin Downey

Distributed under the Eclipse Public License, the same as Clojure.
