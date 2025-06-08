package com.bgpay.bgai.repository.es;

import com.bgpay.bgai.model.es.ChatRecord;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;
 
@Repository
public interface ChatRecordRepository extends ElasticsearchRepository<ChatRecord, String> {
} 