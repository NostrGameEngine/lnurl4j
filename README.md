# lnurl4j

A java client library for LNURL.


The library is released to maven central: [org.ngengine/lnurl4j](https://central.sonatype.com/artifact/org.ngengine/lnurl4j)

```gradle
repositories {
    mavenCentral()
    // Uncomment this if you want to use a -SNAPSHOT version
    //maven { 
    //    url = uri("https://central.sonatype.com/repository/maven-snapshots")
    //}
    maven { // required by NGEPlatform
        url = "https://maven.rblb.it/NostrGameEngine/libdatachannel-java"
    }
}

dependencies {
    implementation 'org.ngengine:lnurl4j:<version>'
}
```


## Status

At the current time, the library does implement only the minimum necessary to provide the functionalities needed by Nostr Game Engine.

This includes basically only the luds related to the payRequest service and the payRequest service itself, you can find a comprehensive list of the implemented luds below:


- [x] lud-01
- [ ] lud-02
- [ ] lud-03
- [ ] lud-04
- [ ] lud-05
- [x] lud-06
- [ ] lud-07
- [ ] lud-08
- [x] lud-09
- [x] lud-10
- [x] lud-11
- [x] lud-12
- [ ] lud-13
- [ ] lud-14
- [ ] lud-15
- [x] lud-16
- [ ] lud-17 : partial 
- [x] lud-18
- [ ] lud-19
- [ ] lud-20
- [x] lud-21


however the library is designed to be extensible and you can easily write an implementation for the missing services that you can then register to the library, using:

```java

LnUrl.registerServiceFactory((data/*request data map*/) -> {
    if (/*check if data is for your service*/) {
        return new YourLnUrlService(data);
    }
    return null; // Return null if not applicable, the library will try the next factory
});

```

## Usage

```java
LnUrl lnurl = new LnUrl(/*lnurl or lud16 url*/);
// or
LnUrl  lnAddress = new LnAddress(/*ln address*/);

LnUrlPay service = (LnUrlPay)lnurl.getService(); /* if it is a lnurlp */
// service.fetchInvoice(....)
```
