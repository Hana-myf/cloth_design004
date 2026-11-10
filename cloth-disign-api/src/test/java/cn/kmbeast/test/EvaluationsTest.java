package cn.kmbeast.test;

import cn.kmbeast.pojo.entity.Evaluations;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class EvaluationsTest extends BaseTest {
    
    @Test
    public void testEvaluationsCreation() {
        // Test evaluations creation
        Evaluations evaluation = new Evaluations();
        evaluation.setId(1);
        evaluation.setParentId(0);
        evaluation.setCommenterId(1);
        evaluation.setReplierId(2);
        evaluation.setContentType("news");
        evaluation.setContent("This is a test comment");
        evaluation.setContentId(1);
        evaluation.setUpvoteList("1,2,3");
        evaluation.setCreateTime(LocalDateTime.now());
        
        assertNotNull(evaluation, "Evaluations object creation failed");
        assertEquals(1, evaluation.getId(), "Evaluations ID setting error");
        assertEquals(0, evaluation.getParentId(), "Parent ID setting error");
        assertEquals(1, evaluation.getCommenterId(), "Commenter ID setting error");
        assertEquals(2, evaluation.getReplierId(), "Replier ID setting error");
        assertEquals("news", evaluation.getContentType(), "Content type setting error");
        assertEquals("This is a test comment", evaluation.getContent(), "Comment content setting error");
        assertEquals(1, evaluation.getContentId(), "Content ID setting error");
        assertEquals("1,2,3", evaluation.getUpvoteList(), "Upvote list setting error");
        assertNotNull(evaluation.getCreateTime(), "Create time setting error");
        
        System.out.println("Evaluations creation test passed");
    }
    
    @Test
    public void testEvaluationsValidation() {
        // Test evaluations validation
        Evaluations evaluation = new Evaluations();
        evaluation.setCommenterId(1);
        evaluation.setContent("This is a test comment");
        evaluation.setContentId(1);
        
        assertNotNull(evaluation.getCommenterId(), "Commenter ID cannot be null");
        assertNotNull(evaluation.getContent(), "Comment content cannot be null");
        assertNotNull(evaluation.getContentId(), "Content ID cannot be null");
        assertTrue(evaluation.getContent().length() > 0, "Comment content length cannot be 0");
        
        System.out.println("Evaluations validation test passed");
    }
}
