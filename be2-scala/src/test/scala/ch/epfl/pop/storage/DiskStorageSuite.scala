package ch.epfl.pop.storage

import java.io.File

import ch.epfl.pop.model.objects.DbActorNAckException
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.{AnyFunSuite => FunSuite}
import org.scalatest.matchers.should.Matchers

import scala.collection.concurrent.TrieMap
import scala.reflect.io.Directory

class DiskStorageSuite extends FunSuite with Matchers with BeforeAndAfterAll {
  // concurrent map storing all instances of DiskStorage
  private val stores: scala.collection.concurrent.Map[String, DiskStorage] = new TrieMap[String, DiskStorage]()

  final val GENERATOR = scala.util.Random

  def key(n: Int = 0): String = s"key$n"
  def value(n: Int = 0): String = s"value$n"

  final val KEY: String = key()
  final val VALUE: String = value()

  final val BATCH: List[(String, String)] = (KEY, VALUE) :: (key(1), value(1)) :: (key(2), value(2)) :: Nil

  override def afterAll(): Unit = {
    for ((storeName, store) <- stores) {
      store.close()

      val directory = new Directory(new File(storeName))
      directory.deleteRecursively()
    }
  }

  private def generateDiskStorage(): DiskStorage = {
    val name: String = s"database-test-${GENERATOR.nextInt(Int.MaxValue)}"
    val storage: DiskStorage = new DiskStorage(name)

    stores += (name -> storage)
    storage
  }

  test("read correctly returns empty Option on non-existing key") {
    val storage: DiskStorage = generateDiskStorage()

    for ((k, _) <- BATCH) storage.read(k) shouldBe empty
  }

  test("read correctly returns successful READ") {
    val storage: DiskStorage = generateDiskStorage()

    storage.write(BATCH: _*)
    storage.read(BATCH.last._1) should equal(Some(BATCH.last._2))
  }

  test("write correctly adds tuples one-by-one in db") {
    val storage: DiskStorage = generateDiskStorage()

    storage.write(KEY -> VALUE)
    storage.read(KEY) should equal(Some(VALUE))

    storage.write(key(1) -> value(1))

    storage.read(KEY) should equal(Some(VALUE))
    storage.read(key(1)) should equal(Some(value(1)))
  }

  test("write correctly adds batch tuples in db") {
    val storage: DiskStorage = generateDiskStorage()

    storage.write(BATCH: _*)

    for ((k, v) <- BATCH) storage.read(k) should equal(Some(v))
  }

  test("write correctly does nothing on no tuple add in db") {
    val storage: DiskStorage = generateDiskStorage()

    storage.write()

    // cannot check the size directly => thus we check a few values that *could* have been added
    for ((k, _) <- BATCH) storage.read(k) shouldBe empty
  }

  test("write correctly aborts on faulty tuple add in db") {
    val storage: DiskStorage = generateDiskStorage()

    val faultyBatch: List[(String, String)] = (KEY, VALUE) :: (null, value(1)) :: (key(2), value(2)) :: Nil
    an[DbActorNAckException] should be thrownBy storage.write(faultyBatch: _*)
  }

  test("all the keys are retrieved") {
    val storage = generateDiskStorage()

    storage.write(BATCH: _*)
    storage.filterKeysByPrefix("key") should equal(BATCH.map(s => s._1).toSet)
  }

  test("Only some of the keys are retrieved") {
    val storage = generateDiskStorage()
    val prefix = "test"

    var set = Set[String]()
    val MIXED_BATCH = BATCH
      .map(s =>
        (
          if (s._1.hashCode % 2 == 0) {
            set += prefix + s._1
            prefix + s._1
          } else
            s._1
          ,
          s._2
        )
      )

    storage.write(MIXED_BATCH: _*)
    storage.filterKeysByPrefix(prefix) should equal(set)
  }

  test("delete successfully deletes a key") {
    val storage: DiskStorage = generateDiskStorage()

    storage.write(KEY -> VALUE)

    noException should be thrownBy storage.delete(KEY)
    storage.delete(KEY) should be((): Unit)
    storage.read(KEY) shouldBe empty
  }

  test("delete successfully handles non-existent key deletion") {
    val storage: DiskStorage = generateDiskStorage()

    noException should be thrownBy storage.delete(KEY)
    storage.delete(KEY) should be((): Unit)
  }
}
