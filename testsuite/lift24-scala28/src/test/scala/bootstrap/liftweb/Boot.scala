package bootstrap.liftweb

import net.liftweb.http.LiftRules
import net.liftweb.sitemap.{SiteMap, Loc, Menu}

/**
 * // TODO: Document this
 * @author Galder Zamarreño
 * @since // TODO
 */
class Boot {

   def boot {
      LiftRules.addToPackages("org.scalabox.lift.helloworld")

      // Build SiteMap
      val entries = Menu(Loc("Home", List("index"), "Home")) :: Nil
      LiftRules.setSiteMap(SiteMap(entries: _*))
   }

}