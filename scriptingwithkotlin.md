# Scripting with Kotlin

## Introduction

There comes a time when we need to do tasks that is either run daily, or just once when we need to. While one can write a full program in Java to deal with the task at hand, it is sometimes simpler to script things up instead. Scripting allows us to make quick changes and run it without the need to compile to a jar file.

[Kotlin](https://kotlinlang.org/), a programming language by the guys behind Jetbrains, is a statically typed programming language for modern multiplatform applications. It is also now a first class supported language for the Android platform. While it's compiled output is normally class or jar files, there is a scripting engine that allows you to run a text file as if it's a script, even though, in the background, you're running actual JVM application. The Kotlin compiler will compile the script in the background and link all the needed libraries if there is a change in the script. It does this all in the background. If the script is not changed, Kotlin would just run the compiled classes instead.

In this post, we are going to create a queue browser in just a few lines of code. The queue browser will dump all the contents of a queue and will continue to subscribe to the queue and dump out new messages as they come.

## Setting up the environment

We have to install a few things before we can get scripting with Kotlin. While Kotlin comes with scripting support, it is not exactly that friendly to use. This guide assumes that you are running on a unix based system, for example, Mac OS X, Linux or even Ubuntu for Windows. It is possible to do this in Windows, but I've not tried it, so your mileage may vary.

You will need to install 3 software components (other than your JDK of choice). These are Kotlin, Maven and `kscript`.

### Installing Kotlin

Based on your operating system, you might want to use your package manager to install Kotlin. The easiest way I find, to install the latest version is to use [SDKMAN](http://sdkman.io/install.html).

``` shell
$ curl -s "https://get.sdkman.io" | bash
$ source "$HOME/.sdkman/bin/sdkman-init.sh"
$ sdk install kotlin

Downloading: kotlin 1.1.3-2

In progress...

######################################################################## 100.0%

Installing: kotlin 1.1.3-2
Done installing!


Setting kotlin 1.1.3-2 as default.
```

Check if you can run `kotlinc`

``` shell
$ kotlinc -version
info: kotlinc-jvm 1.1.3-2 (JRE 1.8.0_144-b01)
```

### Installing Maven

Same as Kotlin, you can either use SDKMAN, or your OS package manager:

``` shell
$ sdk install maven

Downloading: maven 3.5.0

In progress...

######################################################################## 100.0%

Installing: maven 3.5.0
Done installing!

```

To check if you have maven installed:

``` shell
$ mvn -version
Apache Maven 3.5.0 (ff8f5e7444045639af65f6095c62210b5713f426; 2017-04-04T03:39:06+08:00)
Maven home: /usr/local/Cellar/maven/3.5.0/libexec
Java version: 1.8.0_66, vendor: Oracle Corporation
Java home: /Library/Java/JavaVirtualMachines/jdk1.8.0_66.jdk/Contents/Home/jre
Default locale: en_SG, platform encoding: UTF-8
OS name: "mac os x", version: "10.12.5", arch: "x86_64", family: "mac"
```

### `kscript` installation

[kscript](https://github.com/holgerbrandl/kscript) is a convenient wrapper for running Kotlin scripts. It will also integrate with Maven to download libraries for you, for example, the needed Solace JCSMP libraries for this example script. you can use `sdkman` to install kscript:

``` shell
sdk install kscript
Downloading: kscript 2.4.0

In progress...

################################################################# 100.0%

Installing: kscript 2.4.0
Done installing!

Setting kscript 2.4.0 as default.
```

Ensure that you can run `kscript`.

``` shell
$ kscript 
kscript - Enhanced scripting support for Kotlin on *nix-based systems.

Usage:
 kscript [options] <script> [<script_args>]...
 kscript --clear-cache
 kscript --self-update

The <script> can be a  script file (*kts), a script URL, - for stdin, a *.kt source file with a main method, or some kotlin code.

Use '--clear-cache' to wipe cached script jars and urls
Use '--self-update' to update kscript to the latest version

Options:
 -i --interactive        Create interactive shell with dependencies as declared in script
 -t --text               Enable stdin support API for more streamlined text processing
 --idea                  Open script in temporary Intellij session
 -s --silent             Suppress status logging to stderr
 --package               Package script and dependencies into self-dependent binary


Copyright : 2017 Holger Brandl
License   : MIT
Version   : v2.4.0
Website   : https://github.com/holgerbrandl/kscript
```

## Creating our browser script.

Now that we have the installation step out of the way. It is time to create our script.

Like all unix scripts, we begin with the shebang line calling out kscript, we also add another line that will be used by maven to download the Solace libraries and store it in its cache.

``` bash
#!/usr/bin/env kscript
//DEPS com.solacesystems:sol-jcsmp:10.1.1
```

We then have our necessary import statements.

``` kotlin
import com.solacesystems.jcsmp.*
import com.solacesystems.jcsmp.JCSMPProperties.*
```

Initialize our properties to use in our session, and we connect to the appliance or VMR:

``` kotlin
val host = "HOSTNAME_OR_IP_ADDRESS"
val user = "default"
val pass = ""
val vpn  = "default"
val queue = "MY_QUEUE"

val session = JCSMPFactory.onlyInstance().createSession(JCSMPProperties().apply {
  setProperty(HOST, host)
  setProperty(USERNAME, user)
  setProperty(PASSWORD, pass)
  setProperty(VPN_NAME, vpn)
})
session.connect()
```

We will need to subscribe to a queue in order to be able to browse it. Here we create a browser and instantiate with a queue endpoint. We then run the browse function on it.

``` kotlin
val myBrowser = session.createBrowser(BrowserProperties().apply {
  endpoint = JCSMPFactory.onlyInstance().createQueue(queue)
  transportWindowSize = 1
  waitTimeout = 10000
})
browse(myBrowser)
```

In the browse function, we loop forever waiting for a new message in the queue and dump it to screen. If the message is of type TextMessage, print out the text.

``` kotlin
fun browse(browser: Browser) {
  var rx_msg: BytesXMLMessage?
  var count = 0
  do {
      rx_msg = browser.next
      when (rx_msg) {
          null -> {} // Do nothing
          is TextMessage -> {
              count++
              println("#$count\nMessage ID: ${rx_msg.messageId}")
              println("Destination: #${rx_msg.destination.name}")
              println("Message is: ${rx_msg.text}")
              println("--------------")
          }
          else -> {
              count++
              println("Dumping Message #$count\n${rx_msg.dump()}")
              println("--------------")
          }
      }
  } while (true)
}
```

We'll just stop this script with Ctrl-C for now.

## Running the script.

Ensure that the script is executable. This can be done with `chmod +x <scriptname>`. If this is your first time running this program, it may seem to take a very long time to start. In the background, Maven is downloading its indexes and proceeds to download `sol-jcsmp`. This process can take up to 2 minutes. This initial step will only happen once, and subsequent runs will just use the cache from Maven's local cache.
