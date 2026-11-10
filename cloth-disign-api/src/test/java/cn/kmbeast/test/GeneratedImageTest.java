package cn.kmbeast.test;

import cn.kmbeast.pojo.entity.GeneratedImage;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

public class GeneratedImageTest extends BaseTest {
    
    @Test
    public void testGeneratedImageCreation() {
        // Test generated image creation
        GeneratedImage image = new GeneratedImage();
        image.setId(1);
        image.setUserId(1);
        image.setImageName("Test Image");
        image.setImageUrl("https://example.com/image.jpg");
        image.setGenerateTime(new Date());
        
        assertNotNull(image, "Generated image object creation failed");
        assertEquals(1, image.getId(), "Image ID setting error");
        assertEquals(1, image.getUserId(), "User ID setting error");
        assertEquals("Test Image", image.getImageName(), "Image name setting error");
        assertEquals("https://example.com/image.jpg", image.getImageUrl(), "Image URL setting error");
        assertNotNull(image.getGenerateTime(), "Generate time setting error");
        
        System.out.println("Generated image creation test passed");
    }
    
    @Test
    public void testGeneratedImageValidation() {
        // Test generated image validation
        GeneratedImage image = new GeneratedImage();
        image.setUserId(1);
        image.setImageName("Test Image");
        image.setImageUrl("https://example.com/image.jpg");
        
        assertNotNull(image.getUserId(), "User ID cannot be null");
        assertNotNull(image.getImageName(), "Image name cannot be null");
        assertNotNull(image.getImageUrl(), "Image URL cannot be null");
        assertTrue(image.getImageUrl().startsWith("http"), "Image URL format error");
        
        System.out.println("Generated image validation test passed");
    }
}
