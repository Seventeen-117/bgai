package com.bgpay.bgai.model.es;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import java.time.LocalDateTime;

@Data
@Document(indexName = "chat_records")
public class ChatRecord {
    
    @Id
    private String id;

    @Field(type = FieldType.Text)
    private String requestBody;

    @Field(type = FieldType.Text)
    private String responseBody;

    @Field(type = FieldType.Keyword)
    private String traceId;

    @Field(type = FieldType.Keyword)
    private String userId;

    @Field(type = FieldType.Date)
    private LocalDateTime timestamp;

    @Field(type = FieldType.Integer)
    private Integer statusCode;

    @Field(type = FieldType.Text)
    private String errorMessage;

    @Field(type = FieldType.Long)
    private Long processingTime;
} 