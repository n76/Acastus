# Spawn of Acastus
![](https://i.imgur.com/uZYbEwT.png)

## Acastus
[Acastus](https://github.com/DanielBarnett714/Acastus) is/was an open source app for address lookup and sharing your location. It uses a
Pelias backend for address lookup and Mapzen for map display. Unfortunately, the only public Pelias backend disappeared
when Mapzen ceased to exist so Acastus is not very useful at present.

Still, the source code is available and there have been a couple of spinoffs, including [Acastus Photon](https://github.com/gjedeer/Acastus),
so support of the original author is encouraged. Donations to the original author can be made by [Bitcoin](https://blockchain.info/address/1NjjuTxXm3ezpnVUGk4VmdEZUcym3SKZ8z)
or [PayPal](https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=VTUD5XRYMT686&lc=US&item_name=Acastus&currency_code=USD&bn=PP%2dDonationsBF%3abtn_donateCC_LG%2egif%3aNonHosted).

## Spawn of Acastus
This app has a very limited scope born out of the demise of AddressToGPS and a desire to have a way
to inject test addresses into a [microG](https://microg.org) geocoder backend.
* It uses the Google API for address to lat/lon lookup
* It uses the Google Maps API v2 for display of addresses found by the lookup
* Use of Google APIs can correctly be considered "anti-features" but is needed to work with [microG](https://microg.org).
* But it does not have a valid Maps API key so the map functionality does not work on most Android phones.
* It is intended to be run on a phone that has [microG](https://microg.org) installed along with at least one microG address provider plug-ins.

### Screenshots
![](fastlane/metadata/android/en-US/images/Screenshot_1.png)
![](fastlane/metadata/android/en-US/images/Screenshot_2.png)
![](fastlane/metadata/android/en-US/images/Screenshot_3.png)
![](fastlane/metadata/android/en-US/images/Screenshot_4.png)