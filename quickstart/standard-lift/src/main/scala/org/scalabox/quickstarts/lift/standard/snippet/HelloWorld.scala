package org.scalabox.quickstarts.lift.standard.snippet

/**
 * Hello world example.
 *
 * @author Galder Zamarreño
 * @since 1.0
 */
class HelloWorld {

   def howdy =
      <span>
         Hello World from
         <b>ScalaBox</b>
         !
            <br/>
         The date is:
         {new _root_.java.util.Date}
      </span>

}
