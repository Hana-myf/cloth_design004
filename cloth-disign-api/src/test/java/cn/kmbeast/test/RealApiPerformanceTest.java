package cn.kmbeast.test;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class RealApiPerformanceTest extends BaseTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    private static final String API_BASE = "/api/personal-heath/v1.0";
    
    // 测试普通接口的响应时间
    @Test
    public void testRealResponseTime() {
        // 测试新闻列表接口
        long startTime = System.currentTimeMillis();
        
        ResponseEntity<String> response = restTemplate.getForEntity(API_BASE + "/news", String.class);
        
        long endTime = System.currentTimeMillis();
        long responseTime = endTime - startTime;
        
        System.out.println("Real news list API response time: " + responseTime + "ms");
        System.out.println("Response status: " + response.getStatusCode());
        
        assertTrue(response.getStatusCode().is2xxSuccessful(), "API should return success status");
        assertTrue(responseTime < 2000, "Response time should be less than 2 seconds");
    }
    
    // 测试普通接口的并发性能
    @Test
    public void testRealConcurrentUsers() throws InterruptedException {
        int concurrentUsers = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(concurrentUsers);
        CountDownLatch latch = new CountDownLatch(concurrentUsers);
        
        for (int i = 0; i < concurrentUsers; i++) {
            executorService.submit(() -> {
                try {
                    // 测试新闻列表接口
                    ResponseEntity<String> response = restTemplate.getForEntity(API_BASE + "/news", String.class);
                    assertTrue(response.getStatusCode().is2xxSuccessful(), "API should return success status");
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        boolean completed = latch.await(30, TimeUnit.SECONDS);
        executorService.shutdown();
        
        assertTrue(completed, "System should handle " + concurrentUsers + " concurrent users");
        System.out.println("Real concurrent users test passed");
    }
    
    // 测试普通接口的QPS
    @Test
    public void testRealRequestProcessingSpeed() {
        int totalRequests = 1000;
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < totalRequests; i++) {
            try {
                // 测试新闻列表接口
                ResponseEntity<String> response = restTemplate.getForEntity(API_BASE + "/news", String.class);
                assertTrue(response.getStatusCode().is2xxSuccessful(), "API should return success status");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        double requestsPerSecond = totalRequests / (duration / 1000.0);
        
        System.out.println("Real requests per second: " + requestsPerSecond);
        assertTrue(requestsPerSecond > 10, "System should process more than 10 requests per second");
    }
    
    // 测试用户接口
    @Test
    public void testUserApi() {
        long startTime = System.currentTimeMillis();
        
        // 测试用户列表接口
        ResponseEntity<String> response = restTemplate.getForEntity(API_BASE + "/user/list", String.class);
        
        long endTime = System.currentTimeMillis();
        long responseTime = endTime - startTime;
        
        System.out.println("Real user list API response time: " + responseTime + "ms");
        System.out.println("Response status: " + response.getStatusCode());
        
        assertTrue(response.getStatusCode().is2xxSuccessful(), "API should return success status");
    }
    
    // 测试面料接口
    @Test
    public void testFabricApi() {
        long startTime = System.currentTimeMillis();
        
        // 测试面料列表接口
        ResponseEntity<String> response = restTemplate.getForEntity(API_BASE + "/fabric", String.class);
        
        long endTime = System.currentTimeMillis();
        long responseTime = endTime - startTime;
        
        System.out.println("Real fabric list API response time: " + responseTime + "ms");
        System.out.println("Response status: " + response.getStatusCode());
        
        assertTrue(response.getStatusCode().is2xxSuccessful(), "API should return success status");
    }
}
