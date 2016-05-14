slack-ozfindbiz
===============

Slack Outgoing WebHook Integration to search for Australian businesses and POIs using the *Sensis* API.
Runs on *Google App Engine*. 


INSTALL
-------

`$ cp src/main/webapp/config.json.template src/main/webapp/config.json`

Edit *src/main/webapp/config.json* and fill in values for:

* **ozfindbiz.sapi.key** : Go to http://developers.sensis.com.au and register for a Sensis API Test key and paste it here.
* **ozfindbiz.slack.tokens** : Get the token from the Slack Outgoing WebHook integration page and paste it here.
* **ozfindbiz.defaultLocation** : Enter a location that will be searched if a location is not specified by the user. May be a geocode or address text. Must be within Australia.

You may optionally change:

* **ozfindbiz.resultSize** : Number of results to return. Maximum of 20.


`$ cp src/main/webapp/WEB-INF/appengine-web.xml.template src/main/webapp/WEB-INF/appengine-web.xml`

Edit *src/main/webapp/WEB-INF/appengine-web.xml* to fill in your Google App Engine application name within the
`<application></application>` tags.


TEST
----

Start it locally:

`$ gradle appengineRun`

Paste this in your browser

http://localhost:8080/searchBiz?text=/find%20me%20cafes&token=your_slack_token

You should see a JSON formatted text listing some cafes around the default location you've specified.
If it doesn't work, look at the console for errors.


UPLOAD
------
`$ gradle appengineUpdate`


SLACK CONFIGURATION
-------------------
From the Slack service page, create an Outgoing WebHook Integration with the following Google App Engine URL:

http://your-app-name.appspot.com/searchBiz 


USAGE
-----
Examples, if your trigger word is "find me":

* find me cafes
* find me vets in richmond vic
* find me bunnings in melbourne
* find me curry near 222 lonsdale street, melbourne, vic
* find me ATMs in 3150
* find me fish and chips around -38.147518,144.361369
* find me toilets near opera house
* find me petrol stations near chadstone shopping centre, vic

You can replace "find me ", they are treated exactly the same.
If the trigger word contains "map me " instead, it will paste a single Google Static Map link into the Slack response so that the map is fetched.

Note that the *what* part (business type or name) of the query is separated from the *where* part using a join word.
Valid join words are: 

*near*, *in*, *around*, *nearby*, *on*, *at* 

They are all treated exactly the same, there is no semantic difference between them.

The last 2 examples use landmarks as location. Landmarks may be popular tourist sites, shopping centres, big parks, etc. 
Only some landmarks are supported, it is an experimental feature.


TEST TO PROD
------------
When you've finally obtained your SAPI API Prod key, paste it into the 
**ozfindbiz.sapi.key**
configuration in file
*src/main/webapp/config.json*
Then change the 
**ozfindbiz.sapi.mode** 
configuration from *test* to *prod* .


 
