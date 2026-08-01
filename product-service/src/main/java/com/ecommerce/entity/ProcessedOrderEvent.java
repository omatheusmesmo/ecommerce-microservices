package com.ecommerce.entity;

import io.quarkus.mongodb.panache.PanacheMongoEntityBase;
import io.quarkus.mongodb.panache.common.MongoEntity;
import java.time.LocalDateTime;
import org.bson.codecs.pojo.annotations.BsonId;

@MongoEntity(collection = "processed_order_events")
public class ProcessedOrderEvent extends PanacheMongoEntityBase {

    @BsonId
    public String id;

    public LocalDateTime processedAt = LocalDateTime.now();

    public ProcessedOrderEvent() {}

    public ProcessedOrderEvent(String id) {
        this.id = id;
    }
}
