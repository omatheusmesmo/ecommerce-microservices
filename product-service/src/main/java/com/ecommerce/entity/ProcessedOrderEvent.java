package com.ecommerce.entity;

import io.quarkus.mongodb.panache.PanacheMongoEntityBase;
import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.codecs.pojo.annotations.BsonId;

import java.time.LocalDateTime;

@MongoEntity(collection = "processed_order_events")
public class ProcessedOrderEvent extends PanacheMongoEntityBase {

    @BsonId
    public String id;

    public LocalDateTime processedAt = LocalDateTime.now();

    public ProcessedOrderEvent() {
    }

    public ProcessedOrderEvent(String id) {
        this.id = id;
    }
}
