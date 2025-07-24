/*    */ package BOOT-INF.classes.io.tricking.challenge;
/*    */ 
/*    */ import org.springframework.boot.context.properties.ConfigurationProperties;
/*    */ import org.springframework.stereotype.Component;
/*    */ 
/*    */ @Component
/*    */ @ConfigurationProperties(prefix = "keywords")
/*    */ public class KeyworkProperties {
/*    */   private String[] blacklist;
/*    */   
/*    */   public String[] getBlacklist() {
/* 12 */     return this.blacklist;
/*    */   }
/*    */   
/*    */   public void setBlacklist(String[] blacklist) {
/* 16 */     this.blacklist = blacklist;
/*    */   }
/*    */ }


/* Location:              C:\Users\zhr_0\Desktop\challenge-0.0.1-SNAPSHOT.jar!\BOOT-INF\classes\io\tricking\challenge\KeyworkProperties.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */