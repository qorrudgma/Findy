package com.boot.elasticsearch;

import javax.net.ssl.SSLContext;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.ssl.SSLContextBuilder;
import org.elasticsearch.client.RestClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;

@Configuration
public class ElasticsearchConfig {

	@Bean
	public ElasticsearchClient elasticsearchClient() throws Exception {

		// [1] 인증서 검증 무시 (로컬 개발용)
		SSLContext sslContext = SSLContextBuilder.create().loadTrustMaterial((chain, authType) -> true).build();

		// [2] 사용자 인증 설정
		BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
		credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials("elastic", "qwer1234"));
		// 보안 테스트
//		credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials("elastic", "1234"));

		// [3] RestClient 생성
		RestClient restClient = RestClient.builder(new HttpHost("localhost", 9200, "https"))
				.setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder.setSSLContext(sslContext)
						.setDefaultCredentialsProvider(credentialsProvider))
				.build();

		// [4] ElasticsearchClient 생성
		ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());

		return new ElasticsearchClient(transport);
	}
}

//아래는 경음이가 인증서 없는버전으로 만들어둔거 주석처리함 ㅅㄱ
// 인증서 없는 버전
//package com.boot.elasticsearch;
//
//import org.apache.http.HttpHost;
//import org.elasticsearch.client.RestClient;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//import co.elastic.clients.elasticsearch.ElasticsearchClient;
//import co.elastic.clients.json.jackson.JacksonJsonpMapper;
//import co.elastic.clients.transport.rest_client.RestClientTransport;
//
//@Configuration
//public class ElasticsearchConfig {
//
//   @Bean
//   public ElasticsearchClient elasticsearchClient() {
//      // 인증 없는 HTTP 클라이언트 설정
//      RestClient restClient = RestClient.builder(new HttpHost("localhost", 9200, "http")).build();
//
//      RestClientTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
//      return new ElasticsearchClient(transport);
//   }
//}

// 인증서 있는 버전
//package com.boot.elasticsearch;
//
//import java.io.FileInputStream;
//import java.security.KeyStore;
//import java.security.cert.CertificateFactory;
//import java.security.cert.X509Certificate;
//import java.util.Base64;
//
//import javax.net.ssl.SSLContext;
//import javax.net.ssl.TrustManagerFactory;
//
//import org.apache.http.HttpHost;
//import org.apache.http.message.BasicHeader;
//import org.apache.http.ssl.SSLContexts;
//import org.elasticsearch.client.RestClient;
//import org.elasticsearch.client.RestClientBuilder;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//import co.elastic.clients.elasticsearch.ElasticsearchClient;
//import co.elastic.clients.json.jackson.JacksonJsonpMapper;
//import co.elastic.clients.transport.rest_client.RestClientTransport;
//
//@Configuration
//public class ElasticsearchConfig {
//
//	@Value("${spring.elasticsearch.uris}")
//	private String elasticUri;
//
//	@Value("${spring.elasticsearch.username}")
//	private String username;
//
//	@Value("${spring.elasticsearch.password}")
//	private String password;
//
//	@Value("${spring.elasticsearch.ssl.certificate}")
//	private String certPath;
//
//	@Bean
//	public ElasticsearchClient elasticsearchClient() throws Exception {
//		// [1] 인증서 로드
//		FileInputStream fis = new FileInputStream(certPath);
//		CertificateFactory factory = CertificateFactory.getInstance("X.509");
//		X509Certificate caCert = (X509Certificate) factory.generateCertificate(fis);
//
//		// [2] TrustStore 구성
//		KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
//		trustStore.load(null, null);
//		trustStore.setCertificateEntry("ca", caCert);
//
//		TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
//		tmf.init(trustStore);
//
//		// [3] SSLContext 구성
//		SSLContext sslContext = SSLContexts.custom().loadTrustMaterial(trustStore, null).build();
//
//		// [4] 인증 헤더 구성 (Basic Auth)
//		String auth = Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
//
//		RestClientBuilder builder = RestClient.builder(HttpHost.create(elasticUri))
//				.setHttpClientConfigCallback(clientBuilder -> clientBuilder.setSSLContext(sslContext))
//				.setDefaultHeaders(new BasicHeader[] { new BasicHeader("Authorization", "Basic " + auth) });
//
//		RestClient restClient = builder.build();
//		RestClientTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
//		return new ElasticsearchClient(transport);
//	}
//}