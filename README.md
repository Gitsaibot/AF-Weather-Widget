# AF Weather Widget

![af_weather_warm](https://user-images.githubusercontent.com/15521729/111474175-107efa80-872c-11eb-9dd9-11a55146fd31.png)


#### Source code for the Android app.

[<img src="fastlane/metadata/IzzyOnDroid.png" height="60" alt="Get it on IzzyOnDroid">](https://apt.izzysoft.de/fdroid/index/apk/net.gitsaibot.af)

This app is a fork of aix weather widget which is no longer actively developed. It is a compact graphical weather graph as a single row Android widget. The source code is made public domain as it may provide utility for others. Please respect the various APIs used by the app, and please modify the user agent if you are running a modified version of the app.

## Acknowledgements

* The Norwegian Meteorological Institute for providing an [open weather data API](https://api.met.no/#english).
* The National Weather Service for providing an [open weather data API](https://graphical.weather.gov/xml/rest.php).
* The GeoNames database for providing their [timezone and geoname API](http://www.geonames.org/export/web-services.html).
* Thanks to [bharathp666 from DeviantArt](http://bharathp666.deviantart.com/) for the [application icon](http://bharathp666.deviantart.com/art/Android-Weather-Icons-180719113) (`app_icon.png`).

## License

* All code written as part of the app is licensed as [CC0 Universal](https://creativecommons.org/publicdomain/zero/1.0/). The only exceptions are `MultiKey.java` and `Pair.java` which are licensed under Apache 2.0 as specified in their headers.
* The weather icons are owned by The Norwegian Meteorological Institute and are as provided via their [weathericon API](https://api.met.no/weatherapi/weathericon/2.0/documentation).

## Information for use

* Any use of the provided software must respect the terms of each API used.
* [The user agent information must be changed if used in a modified application.](https://github.com/Gitsaibot/AF-Weather-Widget/blob/master/app/src/main/java/net/gitsaibot/af/AixUtils.java#L497)
* [The GeoNames username must be changed if used in a modified application.](https://github.com/Gitsaibot/AF-Weather-Widget/blob/master/app/src/main/java/net/gitsaibot/af/data/AixGeoNamesData.java#L62)
