#!/usr/bin/env kscript
//DEPS com.solacesystems:sol-jcsmp:10.2.0

import com.solacesystems.jcsmp.*
import com.solacesystems.jcsmp.JCSMPProperties.*

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

val myBrowser = session.createBrowser(BrowserProperties().apply {
  endpoint = JCSMPFactory.onlyInstance().createQueue(queue)
  transportWindowSize = 1
  waitTimeout = 10000
})
browse(myBrowser)

fun browse(myBrowser: Browser) {
  var rx_msg: BytesXMLMessage?
  var count = 0
  do {
      rx_msg = myBrowser.next
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
