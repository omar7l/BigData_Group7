package event_generator;

import java.time.Duration;
import java.util.Properties;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

public class EventGenerator {

	public static void main(String[] args) {
		
		/// !!!!!!!!!!!!!!!!!!!!!! Name of group:
		String group = "group0"; //// CHANGE ME!!!!

		// Name of the Kafka topic to publish the events to (please keep the group name
		// as prefix to prevent conflicts with other groups)
		String topic = group + "__orders"; // note: on the first run with a new topic name you will get a waring
													// regarding a failure to fetch metadata. This happens as the stream
													// is only created after the first message was sent.

		// connect to Kafka and create a producer that lets us send events
		Properties props = new Properties();
		props.put("bootstrap.servers", "192.168.111.10:9092");
		props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
		props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

		@SuppressWarnings("resource")
		Producer<String, String> producer = new KafkaProducer<>(props);

		// Now lets send some events:

		// lets assume that we publish an event whenever a product is being sold. The
		// event is structured as follows:
		// - The Event ID is the product ID
		// - The Event content/value is the amount that was sold

		for (;;) { // endless loop

			String key = "product_42"; // always the same product
			String value = "" + Math.floor(Math.random() * 5 + 1); // a random quantity

			// now create and send the event to Kafka
			ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, value);
			producer.send(record, (RecordMetadata metadata, Exception exception) -> {
				if (exception != null) {
					exception.printStackTrace();
				} else {
					System.out.printf("Sent event(key=%s value=%s)%n", key, value);
				}
			});

			// wait a little
			try {
				Thread.sleep(Duration.ofSeconds(2));
			} catch (InterruptedException e) {
			}
		}
	}

}
