MapHelper
=========
[![No Maintenance Intended](http://unmaintained.tech/badge.svg)](http://unmaintained.tech/)

While using Google Maps v2 you need to handle lots of stuff, such as 
  * if Google Play Services are available or not
  * is openGL version is compatible or not
  * SupportMapFragment getMap returns null
  
So i collected them all into one helper class, it is now so easy to use mapView by extending this helper class.
(Any pull requests or improvement suggestions are welcomed.)

General Google Maps
-------------------

You need to change some information in AndroidManifest.xml to have map:

* **Below lines you need to change `com.yayandroid.maphelper` part into your packagename.**

```xml
<permission
        android:name="com.yayandroid.maphelper.permission.MAPS_RECEIVE"
        android:protectionLevel="signature" />
        
<uses-permission android:name="com.yayandroid.maphelper.permission.MAPS_RECEIVE" />
```

* **And here your need to obtain an ApiKey and set it:**

```xml
<meta-data
        android:name="com.google.android.maps.v2.API_KEY"
        android:value="YOUR_API_KEY" />
```

Usage
-----

You just need to extend your fragment from MapHelperFragment, and then override these below methods:

* `onCreateView`
Here you need to inflate your view, and find SupportMapFragment. When you call setMapFragment method it launches a thread which checks google play services availablity, openGL versions and shows errorDialog if necessary. 

* `onMapReady`
Thread finally finds map, so it will call this method. You can now do your map settings, put markers and so on.
Here you can also activate myLocation. You can call getMyLocation or moveToMyLocation methods. getMyLocation finds eventually user's location but it is not guaranteed that camera will move to there, so if that is what you want then you need to call moveToMyLocation.

* `noMapFound`
Or if thread can't find any map, calls this method. So you can notify user here about error.

* `onMyLocationReady`
If you called getMyLocation or moveToMyLocation, then this method will be called when they finally find user's location
Here you can request user's current city, county, country so on. To do that, you need to call getLocationInfo method.

* `onLocationInfoReceived`
If you called getLocationInfo method, then it will call this method when it finds user's location informations. This method gets called once for every field that you requested.


Used Libraries
--------------

[commonguys AbstractMapActivity][1]

[GeoCoderHelper class][2]


License
-----------

    Copyright 2013 yayandroid

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

[1]: https://github.com/commonsguy/cw-omnibus/blob/master/MapsV2/Popups/src/com/commonsware/android/mapsv2/popups/AbstractMapActivity.java
[2]: http://stackoverflow.com/a/15853124/1171484
