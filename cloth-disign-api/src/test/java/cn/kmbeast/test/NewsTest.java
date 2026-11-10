package cn.kmbeast.test;

import cn.kmbeast.pojo.entity.News;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class NewsTest extends BaseTest {
    
    @Test
    public void testNewsCreation() {
        // Test news creation
        News news = new News();
        news.setName("Test News");
        news.setContent("This is a test news content");
        news.setTagId(12);
        news.setCover("https://example.com/cover.jpg");
        news.setReaderIds("1,2,3");
        news.setIsTop(false);
        news.setCreateTime(LocalDateTime.now());
        
        assertNotNull(news, "News object creation failed");
        assertEquals("Test News", news.getName(), "News title setting error");
        assertEquals("This is a test news content", news.getContent(), "News content setting error");
        assertEquals(12, news.getTagId(), "Tag ID setting error");
        assertEquals("https://example.com/cover.jpg", news.getCover(), "Cover image setting error");
        assertEquals("1,2,3", news.getReaderIds(), "Reader ID list setting error");
        assertFalse(news.getIsTop(), "Top status setting error");
        assertNotNull(news.getCreateTime(), "Create time setting error");
        
        System.out.println("News creation test passed");
    }
    
    @Test
    public void testNewsValidation() {
        // Test news validation
        News news = new News();
        news.setName("Test News");
        news.setContent("This is a test news content");
        
        assertNotNull(news.getName(), "News title cannot be null");
        assertNotNull(news.getContent(), "News content cannot be null");
        assertTrue(news.getName().length() > 0, "News title length cannot be 0");
        assertTrue(news.getContent().length() > 0, "News content length cannot be 0");
        
        System.out.println("News validation test passed");
    }
}
