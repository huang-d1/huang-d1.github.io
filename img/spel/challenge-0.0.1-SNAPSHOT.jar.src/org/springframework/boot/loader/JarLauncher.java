/*    */ package org.springframework.boot.loader;
/*    */ 
/*    */ import org.springframework.boot.loader.archive.Archive;
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ public class JarLauncher
/*    */   extends ExecutableArchiveLauncher
/*    */ {
/*    */   static final String BOOT_INF_CLASSES = "BOOT-INF/classes/";
/*    */   static final String BOOT_INF_LIB = "BOOT-INF/lib/";
/*    */   
/*    */   public JarLauncher() {}
/*    */   
/*    */   protected JarLauncher(Archive archive) {
/* 39 */     super(archive);
/*    */   }
/*    */ 
/*    */   
/*    */   protected boolean isNestedArchive(Archive.Entry entry) {
/* 44 */     if (entry.isDirectory()) {
/* 45 */       return entry.getName().equals("BOOT-INF/classes/");
/*    */     }
/* 47 */     return entry.getName().startsWith("BOOT-INF/lib/");
/*    */   }
/*    */   
/*    */   public static void main(String[] args) throws Exception {
/* 51 */     (new JarLauncher()).launch(args);
/*    */   }
/*    */ }


/* Location:              C:\Users\zhr_0\Desktop\challenge-0.0.1-SNAPSHOT.jar!\org\springframework\boot\loader\JarLauncher.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */