package event_generator;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.bson.Document;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class FitFuelEventGenerator {
    private static Producer<String, String> producer;
   
    
    public static void main(String[] args) {
        // Kafka Configuration
        String group = "group7"; // Group name from your MongoDB Compass setup
      
        // Kafka Producer properties
        Properties props = new Properties();
        props.put("bootstrap.servers", "192.168.111.10:9092");
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        producer = new KafkaProducer<>(props);

        // MongoDB Connection using MongoClients with authentication
        String mongoUri = "mongodb://group7:o934n343qc30r@192.168.111.4:27017/group7";  
        MongoClient mongoClient = MongoClients.create(mongoUri);
        
       
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
        	// Keep track of the last processed review_id
        	
        	try {
                //System.out.println("Starting stock alert check...");

                MongoDatabase database = mongoClient.getDatabase("group7");

                // You can add the other events here as needed (Purchase, Review, Stock etc.)
                
                //generateProductStockAlertEvent(database, group + "__stock_alerts");              
             
                generateProductPurchasedEvent(database,group + "__Purchase_alerts");// Topic for purchase alerts
                
                //generateProductReviewEvent(database,group + "__review_alerts"); // Topic for review alerts
                
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("Error occurred in scheduled task: " + e.getMessage());
            }
        	
        	
        	
        }, 0, 1, TimeUnit.SECONDS);
        
        
        
        

        try {
            TimeUnit.HOURS.sleep(1);  // Keeps the application alive for 1 hour
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            // Close the Kafka producer and MongoDB connection after the period
            producer.close();
            mongoClient.close();
            scheduler.shutdown();
        }
    }
    
 // Keep track of the last processed order_id
    private static int lastProcessedOrderId = 0;
    
    
    public static void generateProductStockAlertEvent(MongoDatabase database, String topic) {
        // Define a critical stock threshold
        int criticalThreshold = 10;
    	MongoCollection<Document> productsCollection = database.getCollection("products");
    	// Loop through the products and check stock levels
    	//System.out.println("blabla");
   
        for (Document product : productsCollection.find()) {

            int stockLevel = product.getInteger("stock_level");
            if (stockLevel <= criticalThreshold) {
                // Create a stock alert event for products below the threshold
                Integer productId = product.getInteger("product_id");
                String productName = product.getString("product_name");
                String stockAlertEvent = String.format("{\"product_id\":\"%s\", \"product_name\":\"%s\", \"current_stock_level\":%d, \"threshold\":%d}",
                        productId, productName, stockLevel, criticalThreshold);
              
                // Send the event to Kafka topic "stock_alerts"
                producer.send(new ProducerRecord<>(topic, productId.toString(), stockAlertEvent));
                System.out.println("Stock alert event sent: " + stockAlertEvent);
            }
        }
    }


    // Generate and send ProductPurchasedEvent for newly added orders
    public static void generateProductPurchasedEvent(MongoDatabase database, String topic) {

        MongoCollection<Document> ordersCollection = database.getCollection("orders");

        // Query for orders with order_id greater than the last processed order_id
        for (Document order : ordersCollection.find(new Document("order_id", new Document("$gt", lastProcessedOrderId)))) {
            Integer orderId = order.getInteger("order_id");
            Integer customerId = order.getInteger("customer_id");
            String orderDate = order.getString("order_date");

            // Assuming `products` is a list of product IDs and quantities in the order
            for (Document product : (Iterable<Document>) order.get("products")) {
                Integer productId = product.getInteger("product_id");
                String productCategory = product.getString("category");
                Integer quantity = product.getInteger("quantity");
                Integer price = product.getInteger("price");

                // Create the purchase event message
                String purchaseEvent = String.format(
                    "{\"order_id\":%d, \"product_id\":%d, \"product_category\":\"%s\", \"customer_id\":%d, \"quantity\":%d, \"price\":%d, \"order_date\":\"%s\"}",
                    orderId, productId, productCategory, customerId, quantity, price, orderDate
                );

                // Send the event to Kafka
                producer.send(new ProducerRecord<>(topic, orderId.toString(), purchaseEvent));
                System.out.println("ProductPurchasedEvent sent: " + purchaseEvent);
            }

            // Update the lastProcessedOrderId to the current order_id
            lastProcessedOrderId = orderId;
        }
    }
    
    private static int lastProcessedReviewId = 0;
    
    public static void generateProductReviewEvent(MongoDatabase database, String topic) {
        MongoCollection<Document> reviewsCollection = database.getCollection("reviews");

        // Query for reviews with review_id greater than the last processed review_id
        for (Document review : reviewsCollection.find(new Document("review_id", new Document("$gt", lastProcessedReviewId)))) {
            Integer reviewId = review.getInteger("review_id");
            Integer productId = review.getInteger("product_id");
            Integer customerId = review.getInteger("customer_id");
            int rating = review.getInteger("rating");
            String reviewDate = review.getString("review_date");

            // Create the review event message
            String reviewEvent = String.format(
                "{\"review_id\":%d, \"product_id\":%d, \"customer_id\":%d, \"rating\":%d, \"review_date\":\"%s\"}",
                reviewId, productId, customerId, rating, reviewDate
            );

            // Send the event to Kafka
            producer.send(new ProducerRecord<>(topic, reviewId.toString(), reviewEvent));
            System.out.println("ProductReviewEvent sent: " + reviewEvent);

            // Update the lastProcessedReviewId to the current review_id
            lastProcessedReviewId = reviewId;
        }
    }
    
    
}
