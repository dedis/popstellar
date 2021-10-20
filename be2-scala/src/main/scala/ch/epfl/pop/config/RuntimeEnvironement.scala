package ch.epfl.pop.config

import java.io.File
import com.typesafe.config.{Config, ConfigFactory}
import scala.sys.SystemProperties
import akka.event.slf4j.Logger
import java.util.logging
import java.io.IOException
import java.util.regex.Pattern

/** 
  * RuntimeConfiguration object provider This object provides application config
  * for setting up akka http/actor environemnt
  *
  * @see [[config/application.conf]]
  * @see [[ServerConf]]
  */
object RuntimeEnvironment {
  
  private lazy val sp = new SystemProperties()
  
  private def getConfDir: String = {

    val path_wd = sp("user.dir")
    val wd = path_wd.split(Pattern.quote(File.separator)).last
    
    if( wd != "be2-scala"){
        throw new Error("Please check that your current working directory is path\\to\\be2-scala\\")
    }

    /*Build config directory path based be2-scala*/
    val builder = new StringBuilder(path_wd).append(File.separator);
    Seq("src","main","scala","ch","epfl","pop","config").foldLeft(builder){case (b,s) => b.append(s + File.separator)}.toString()

  }

  private lazy val appConfFile =
    getConfDir + "application.conf"

  lazy val appConf: Config = ConfigFactory.parseFile(new File(appConfFile))
    

}
