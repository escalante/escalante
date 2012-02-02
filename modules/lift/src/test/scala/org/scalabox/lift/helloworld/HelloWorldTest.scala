package org.scalabox.lift.helloworld

import org.jboss.arquillian.junit.Arquillian
import org.junit.runner.RunWith
import org.jboss.shrinkwrap.api.spec.WebArchive
import org.jboss.arquillian.container.test.api.Deployment
import org.jboss.shrinkwrap.api.asset.{StringAsset, Asset}
import snippet.HelloWorld
import xml.Elem
import bootstrap.liftweb.Boot
import java.net.URL
import java.io.{BufferedInputStream, StringWriter}
import org.jboss.shrinkwrap.resolver.api.DependencyResolvers
import org.jboss.shrinkwrap.resolver.api.maven.MavenDependencyResolver
import org.jboss.shrinkwrap.api.{GenericArchive, ShrinkWrap}
import org.scalabox.util.Closeable
import org.scalabox.util.Closeable._
import org.scalabox.logging.Log
import org.junit.Test

/**
 * // TODO: Document this
 * @author Galder Zamarreño
 * @since // TODO
 */
@RunWith(classOf[Arquillian])
class HelloWorldTest {

   @Test def testHelloWorld = performHttpCall("localhost", 8080, "helloworld")

   private def performHttpCall(host: String, port: Int, context: String) {
      use(new StringWriter) { writer =>
         val url = new URL("http://" + host + ":" + port + "/" + context + "/index.html")
         println("Reading response from " + url + ":")
         val con = url.openConnection
         use(new BufferedInputStream(con.getInputStream)) { in =>
            var i = in.read
            while (i != -1) {
               writer.write(i.asInstanceOf[Char])
               i = in.read
            }
            val rsp = writer.toString
            assert(rsp.indexOf("Hello World!") > -1, rsp)
            println("OK")
         }
      }
   }

}

object HelloWorldTest extends Log {

   @Deployment def deployment: WebArchive = {
      info("Create war deployment")

      val war = ShrinkWrap.create(classOf[WebArchive], "helloworld.war")
      val indexHtml = xml {
         <lift:surround with="default" at="content">
          <h2>Welcome to ScalaBox!</h2>
          <p><lift:helloWorld.howdy /></p>
         </lift:surround>
      }

      val defaultHtml = xml {
         <html xmlns="http://www.w3.org/1999/xhtml"
               xmlns:lift="http://liftweb.net/">
            <head>
              <meta http-equiv="content-type"
                    content="text/html; charset=UTF-8" />
              <meta name="description" content="" />
              <meta name="keywords" content="" />

              <title>org.scalabox.lift.helloworld:helloworld:1.0-SNAPSHOT</title>
              <script id="jquery" src="/classpath/jquery.js"
                      type="text/javascript"></script>
            </head>
            <body>
              <lift:bind name="content" />
              <lift:Menu.builder />
              <lift:msgs/>
            </body>
         </html>
      }
      
      val liftXml = xml {
         <lift-app version="2.4" />
      }

      // TODO: How avoid duplicating library version?? Load from a pom...
      war.addAsWebResource(indexHtml, "index.html")
         .addAsWebResource(defaultHtml, "templates-hidden/default.html")
         .addAsWebResource(liftXml, "WEB-INF/lift.xml")
         // TODO Why do I need to add test superclass manually?? - classOf[AbstractLiftTest]
         .addClasses(classOf[Boot], classOf[HelloWorld], classOf[Closeable])
         .addAsLibraries(DependencyResolvers.use(classOf[MavenDependencyResolver])
            .artifacts("org.scalatest:scalatest_2.9.0:1.6.1")
                  .exclusion("org.scala-lang:scala-library")
            .resolveAs(classOf[GenericArchive])
         )

      info("War deployment created")

      return war
   }

   private def xml(e: Elem): Asset = new StringAsset(e.toString())

}