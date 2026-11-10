package cn.kmbeast.test;

import cn.kmbeast.pojo.entity.Fabric;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

public class FabricTest extends BaseTest {
    
    @Test
    public void testFabricCreation() {
        // Test fabric creation
        Fabric fabric = new Fabric();
        fabric.setId(1L);
        fabric.setName("Test Fabric");
        fabric.setType("Cotton");
        fabric.setImageUrl("https://example.com/fabric.jpg");
        fabric.setDescription("This is a test fabric");
        fabric.setCreateTime(new Date());
        fabric.setUpdateTime(new Date());
        
        assertNotNull(fabric, "Fabric object creation failed");
        assertEquals(1L, fabric.getId(), "Fabric ID setting error");
        assertEquals("Test Fabric", fabric.getName(), "Fabric name setting error");
        assertEquals("Cotton", fabric.getType(), "Fabric type setting error");
        assertEquals("https://example.com/fabric.jpg", fabric.getImageUrl(), "Fabric image URL setting error");
        assertEquals("This is a test fabric", fabric.getDescription(), "Fabric description setting error");
        assertNotNull(fabric.getCreateTime(), "Create time setting error");
        assertNotNull(fabric.getUpdateTime(), "Update time setting error");
        
        System.out.println("Fabric creation test passed");
    }
    
    @Test
    public void testFabricValidation() {
        // Test fabric validation
        Fabric fabric = new Fabric();
        fabric.setName("Test Fabric");
        fabric.setType("Cotton");
        
        assertNotNull(fabric.getName(), "Fabric name cannot be null");
        assertNotNull(fabric.getType(), "Fabric type cannot be null");
        assertTrue(fabric.getName().length() > 0, "Fabric name length cannot be 0");
        assertTrue(fabric.getType().length() > 0, "Fabric type length cannot be 0");
        
        System.out.println("Fabric validation test passed");
    }
}
