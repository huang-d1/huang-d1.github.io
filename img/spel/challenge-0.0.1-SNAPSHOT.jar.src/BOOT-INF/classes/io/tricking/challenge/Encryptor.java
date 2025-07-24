/*    */ package BOOT-INF.classes.io.tricking.challenge;
/*    */ 
/*    */ import java.util.Base64;
/*    */ import javax.crypto.Cipher;
/*    */ import javax.crypto.spec.IvParameterSpec;
/*    */ import javax.crypto.spec.SecretKeySpec;
/*    */ import org.slf4j.Logger;
/*    */ import org.slf4j.LoggerFactory;
/*    */ 
/*    */ public class Encryptor {
/* 11 */   static Logger logger = LoggerFactory.getLogger(io.tricking.challenge.Encryptor.class);
/*    */   
/*    */   public static String encrypt(String key, String initVector, String value) {
/*    */     try {
/* 15 */       IvParameterSpec iv = new IvParameterSpec(initVector.getBytes("UTF-8"));
/* 16 */       SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");
/*    */       
/* 18 */       Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
/* 19 */       cipher.init(1, skeySpec, iv);
/*    */       
/* 21 */       byte[] encrypted = cipher.doFinal(value.getBytes());
/*    */       
/* 23 */       return Base64.getUrlEncoder().encodeToString(encrypted);
/* 24 */     } catch (Exception ex) {
/* 25 */       logger.warn(ex.getMessage());
/*    */ 
/*    */       
/* 28 */       return null;
/*    */     } 
/*    */   }
/*    */   public static String decrypt(String key, String initVector, String encrypted) {
/*    */     try {
/* 33 */       IvParameterSpec iv = new IvParameterSpec(initVector.getBytes("UTF-8"));
/* 34 */       SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");
/*    */       
/* 36 */       Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
/* 37 */       cipher.init(2, skeySpec, iv);
/*    */       
/* 39 */       byte[] original = cipher.doFinal(Base64.getUrlDecoder().decode(encrypted));
/*    */       
/* 41 */       return new String(original);
/* 42 */     } catch (Exception ex) {
/* 43 */       logger.warn(ex.getMessage());
/*    */ 
/*    */       
/* 46 */       return null;
/*    */     } 
/*    */   }
/*    */ }


/* Location:              C:\Users\zhr_0\Desktop\challenge-0.0.1-SNAPSHOT.jar!\BOOT-INF\classes\io\tricking\challenge\Encryptor.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */