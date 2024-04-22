package ch.epfl.pop.model.objects

import org.scalatest.funsuite.{AnyFunSuite => FunSuite}
import org.scalatest.matchers.should.Matchers
import spray.json._

class RumorDataSuite extends FunSuite with Matchers {
  
  test("Json conversion works for RumorDataSuite"){
    val rumorData = RumorData(List(1,2,3,4,5))
    
    val rumorDataJson = RumorData.buildFromJson(rumorData.toJsonString)
    
    rumorData should equal(rumorDataJson)
  }
  
  test("RumorData updates") {
    val rumorData = RumorData(List(1,2,3,4,5))
    
    val updatedRumorData = rumorData.updateWith(6)
    
    updatedRumorData.rumorIds should equal(List(1,2,3,4,5,6))
    
  }

}
