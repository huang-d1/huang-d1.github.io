/*    */ package org.springframework.boot.loader;
/*    */ 
/*    */ import java.util.ArrayList;
/*    */ import java.util.List;
/*    */ import java.util.jar.Manifest;
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
/*    */ public abstract class ExecutableArchiveLauncher
/*    */   extends Launcher
/*    */ {
/*    */   private final Archive archive;
/*    */   
/*    */   public ExecutableArchiveLauncher() {
/*    */     try {
/* 38 */       this.archive = createArchive();
/*    */     }
/* 40 */     catch (Exception ex) {
/* 41 */       throw new IllegalStateException(ex);
/*    */     } 
/*    */   }
/*    */   
/*    */   protected ExecutableArchiveLauncher(Archive archive) {
/* 46 */     this.archive = archive;
/*    */   }
/*    */   
/*    */   protected final Archive getArchive() {
/* 50 */     return this.archive;
/*    */   }
/*    */ 
/*    */   
/*    */   protected String getMainClass() throws Exception {
/* 55 */     Manifest manifest = this.archive.getManifest();
/* 56 */     String mainClass = null;
/* 57 */     if (manifest != null) {
/* 58 */       mainClass = manifest.getMainAttributes().getValue("Start-Class");
/*    */     }
/* 60 */     if (mainClass == null) {
/* 61 */       throw new IllegalStateException("No 'Start-Class' manifest entry specified in " + this);
/*    */     }
/*    */     
/* 64 */     return mainClass;
/*    */   }
/*    */ 
/*    */ 
/*    */   
/*    */   protected List<Archive> getClassPathArchives() throws Exception {
/* 70 */     List<Archive> archives = new ArrayList<>(this.archive.getNestedArchives(this::isNestedArchive));
/* 71 */     postProcessClassPathArchives(archives);
/* 72 */     return archives;
/*    */   }
/*    */   
/*    */   protected abstract boolean isNestedArchive(Archive.Entry paramEntry);
/*    */   
/*    */   protected void postProcessClassPathArchives(List<Archive> archives) throws Exception {}
/*    */ }


/* Location:              C:\Users\zhr_0\Desktop\challenge-0.0.1-SNAPSHOT.jar!\org\springframework\boot\loader\ExecutableArchiveLauncher.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */