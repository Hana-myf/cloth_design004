package cn.kmbeast.test;

import cn.kmbeast.pojo.entity.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class UserTest extends BaseTest {
    
    @Test
    public void testUserCreation() {
        // Test user creation
        User user = User.builder()
                .userAccount("test@example.com")
                .userName("Test User")
                .userPwd("password123")
                .userAvatar("https://example.com/avatar.jpg")
                .userEmail("test@example.com")
                .userRole(1)
                .isLogin(true)
                .isWord(false)
                .build();
        
        assertNotNull(user, "User object creation failed");
        assertEquals("test@example.com", user.getUserAccount(), "User account setting error");
        assertEquals("Test User", user.getUserName(), "User name setting error");
        assertEquals(1, user.getUserRole(), "User role setting error");
        assertTrue(user.getIsLogin(), "User login status setting error");
        assertFalse(user.getIsWord(), "User mute status setting error");
        
        System.out.println("User creation test passed");
    }
    
    @Test
    public void testUserValidation() {
        // Test user validation
        User user = new User();
        user.setUserAccount("test@example.com");
        user.setUserPwd("password123");
        
        assertNotNull(user.getUserAccount(), "User account cannot be null");
        assertNotNull(user.getUserPwd(), "User password cannot be null");
        assertTrue(user.getUserAccount().contains("@"), "User account format error");
        assertTrue(user.getUserPwd().length() >= 6, "User password length is insufficient");
        
        System.out.println("User validation test passed");
    }
}
