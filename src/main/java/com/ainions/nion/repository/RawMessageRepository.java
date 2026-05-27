package com.ainions.nion.repository;

import com.ainions.nion.document.RawMessageDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RawMessageRepository extends MongoRepository<RawMessageDocument, String> {
}
