//package com.boot.elasticsearch;
//
//import org.bson.Document;
//import org.springframework.stereotype.Service;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.data.mongodb.core.MongoTemplate;
//
//@Service
//public class MongoTestService {
//
//    private final MongoTemplate mongoTemplate;
//
//    @Autowired
//    public MongoTestService(MongoTemplate mongoTemplate) {
//        this.mongoTemplate = mongoTemplate;
//    }
//
//    public void testConnection() {
//        try {
//            String dbName = mongoTemplate.getDb().getName();
//            System.out.println("MongoDB 연결 성공! 현재 DB 이름: " + dbName);
//            
//            Document doc = new Document("test", "value");
//            mongoTemplate.save(doc, "sample_collection");
//
//            System.out.println("'sample_collection'에 테스트 데이터 insert 완료!");
//        } catch (Exception e) {
//            System.err.println("MongoDB 연결 실패: " + e.getMessage());
//        }
//    }
//}
