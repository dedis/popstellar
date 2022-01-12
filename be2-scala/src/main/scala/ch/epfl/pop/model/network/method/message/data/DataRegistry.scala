package ch.epfl.pop.model.network.method.message.data

import scala.util.Try

import scala.collection.immutable.Map
import ch.epfl.pop.model.network.method.message.data.ObjectType.ObjectType
import ch.epfl.pop.model.network.method.message.data.ActionType.ActionType

/**
  * Class representing a key entry pair to [[DataRegistry]] mapping
  * @param obj first member of the pair
  * @param action second member of the pair
  */
sealed case class EntryPair(final val obj: ObjectType, final val action: ActionType)

/**
  * Class representing Metadata of a specific action and object type
  * @param schemaValidator schema validation function
  * @param buildFromJson parser from payload to MessageData
  * @param errMessage errMessage to send back in case of schema validation failure
  */
sealed case class MetaData(final val schemaValidator: String => Try[Unit], final val buildFromJson: String => MessageData, final val errMessage: String)

/**
  * Class encapsulating a mapping, offers methods utility methods to extract sub-mappings from main one
  * @param mapping EntryPair to Metadata map
  */
sealed case class DataRegistry(private val mapping : Map[EntryPair, MetaData]){

  /**
  * @param obj objectType
  * @param action actionType
  * @return Metadata corresponding to the (obj, action) key
  */
  def getMetaData(obj: ObjectType, action: ActionType): MetaData = mapping.get(EntryPair(obj, action)) match {
    case Some(metadata) => metadata
    case None => throw new IllegalStateException(s"Metadata for ($obj, $action) unsupported or not added yet in DataRegisteryModule")
  }

  /**
    * @param obj objectType
    * @return immuatble map from the main one that contains only mappings (ActionType -> Metadata)
    * corresponding to ObjectType
    */
  def getFromObject(obj: ObjectType): Map[ActionType, MetaData] =
    mapping.foldLeft(Map.empty[ActionType, MetaData]){
      case (acc, value @ (EntryPair(o,a), metadata) ) if(o == obj) => acc + (a -> metadata)
      case (acc, _) => acc
    }
 }

 /**Companion object of DataRegistry Class contains Builder**/
case object DataRegistry {
  /**
    * DataRegistry Builder
    */
  case class Builder() {
    private val builderMapping  = Map.newBuilder[EntryPair, MetaData]

    /**
      * Add key-value to builder map
      * @param obj objectType
      * @param action actionType
      * @param schemaValidator specific schema validator for object & action
      * @param buildFromJson specific payload parser for object & action
      * @return
      */
    def add(obj: ObjectType, action: ActionType, schemaValidator: String => Try[Unit], buildFromJson:  String => MessageData)(errMesg: String): Builder = {
      builderMapping += (EntryPair(obj, action) ->  MetaData(schemaValidator, buildFromJson, errMesg))
      this
    }
    /**
      * @return Builds DataRegistry
      */
    def build: DataRegistry = DataRegistry(builderMapping.result)

  }
}
