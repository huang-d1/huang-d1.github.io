/*    */ package BOOT-INF.classes.io.tricking.challenge.spel;
/*    */ 
/*    */ import java.util.Collections;
/*    */ import java.util.List;
/*    */ import org.springframework.expression.ConstructorResolver;
/*    */ import org.springframework.expression.spel.support.StandardEvaluationContext;
/*    */ 
/*    */ public class SmallEvaluationContext extends StandardEvaluationContext {
/*    */   public void setConstructorResolvers(List<ConstructorResolver> constructorResolvers) {}
/*    */   
/*    */   public List<ConstructorResolver> getConstructorResolvers() {
/* 12 */     return Collections.emptyList();
/*    */   }
/*    */ }


/* Location:              C:\Users\zhr_0\Desktop\challenge-0.0.1-SNAPSHOT.jar!\BOOT-INF\classes\io\tricking\challenge\spel\SmallEvaluationContext.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */