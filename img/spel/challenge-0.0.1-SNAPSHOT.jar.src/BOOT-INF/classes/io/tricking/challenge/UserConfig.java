/*    */ package BOOT-INF.classes.io.tricking.challenge;
/*    */ 
/*    */ import io.tricking.challenge.Encryptor;
/*    */ import org.springframework.boot.context.properties.ConfigurationProperties;
/*    */ import org.springframework.stereotype.Component;
/*    */ 
/*    */ @Component
/*    */ @ConfigurationProperties(prefix = "user")
/*    */ public class UserConfig
/*    */ {
/*    */   private String username;
/*    */   private String password;
/*    */   private String rememberMeKey;
/*    */   
/*    */   public String getUsername() {
/* 16 */     return this.username;
/*    */   }
/*    */   
/*    */   public String getPassword() {
/* 20 */     return this.password;
/*    */   }
/*    */   
/*    */   public String getRememberMeKey() {
/* 24 */     return this.rememberMeKey;
/*    */   }
/*    */   
/*    */   public void setUsername(String username) {
/* 28 */     this.username = username;
/*    */   }
/*    */   
/*    */   public void setPassword(String password) {
/* 32 */     this.password = password;
/*    */   }
/*    */   
/*    */   public void setRememberMeKey(String rememberMeKey) {
/* 36 */     this.rememberMeKey = rememberMeKey;
/*    */   }
/*    */   
/*    */   public String encryptRememberMe() {
/* 40 */     String encryptd = Encryptor.encrypt(this.rememberMeKey, "0123456789abcdef", this.username);
/* 41 */     return encryptd;
/*    */   }
/*    */   
/*    */   public String decryptRememberMe(String encryptd) {
/* 45 */     return Encryptor.decrypt(this.rememberMeKey, "0123456789abcdef", encryptd);
/*    */   }
/*    */ }


/* Location:              C:\Users\zhr_0\Desktop\challenge-0.0.1-SNAPSHOT.jar!\BOOT-INF\classes\io\tricking\challenge\UserConfig.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */