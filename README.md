# The Open Rights Group Blocked! Android Client #

## How To Help Bring Transparency To UK Internet Filtering ##

* Install the app from [Google Play](https://play.google.com/store/apps/details?id=uk.org.blocked.app) or fork this repo and install yourself via [Android Studio](https://developer.android.com/sdk/installing/studio.html)
* URLs are sent to your phone/tablet from the Blocked! servers *(URLs are found from social media or user submissions on https://blocked.org.uk)*
* Your device issues a [HTTP GET request](https://developer.android.com/reference/org/apache/http/client/methods/HttpGet.html) and then reports back if it could reach the URL or not
* The Blocked! servers retain this information to help map the spread of online censorship
* Censorship data may also passed onto the [Open Observatory of Network Interference](https://ooni.torproject.org/)

## What's the problem? ##

Filtering technologies are being promoted by government as a means to prevent children from accessing inappropriate sexual material. However in practice, many more people are finding themselves behind filters, which block a much wider range of material. Sometimes, like blogs or forums, this may be deliberate but not really necessary, while at other times, it can be by mistake.

This makes filters unnecessarily disruptive to the and very difficult for website owners to understand what is happening, and correct or limit damage.

###Other censorship###

Other censorship is mandatory but not well documented, starting with copyright blocks. Here, court orders allow the claimant to order ISPs to block any domain or sometimes IP address that is serving copies of the original. Orders are usually drafted to be indefinite and blocking pages do not explain the legal basis not how to complain or correct mistakes.

This will have some impact on the reports we receive of blocks on networks.

##Opportunities##

* Making the real behaviour of these filters transparent;
* Helping website owners check whether a given filter has blocked their site erroneously;
* Improving processes for correcting censorship mistakes;
* Improving the supervision of children online by informing society about filter effectiveness;
* Reducing the potential for infringement of the rights of children (and others) to access information and express themselves online;
* Stimulating and informing public debate about the pros and cons of default-on web filtering;
* Researching and documenting censorship methods and technologies;
* Producing code and data that can be reused by others.