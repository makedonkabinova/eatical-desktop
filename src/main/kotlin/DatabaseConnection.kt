import com.mongodb.BasicDBObject
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import org.bson.Document
import org.litote.kmongo.*

class DatabaseConnection(url: String, name: String) {
    private val client: MongoClient
    private val database: MongoDatabase

    init{
        client = KMongo.createClient(url)
        database = client.getDatabase(name)
    }

    fun insertRestaurant(document: Document){
        val collection = this.database.getCollection("restaurants")
        collection.insertOne(document)
    }

    fun updateRestaurant(newDocumentValue: BasicDBObject, restaurantId: Id<String>){
        val collection = this.database.getCollection("restaurants")
        val query = BasicDBObject()
        query.put("_id", restaurantId) //filtering the restaurant that needs update

        val updateObject = BasicDBObject()
        updateObject.put("\$set", newDocumentValue)

        collection.updateOne(query, updateObject)
    }

    fun getRestaurants(): MongoCollection<Restaurant> = database.getCollection<Restaurant>("restaurants")
}