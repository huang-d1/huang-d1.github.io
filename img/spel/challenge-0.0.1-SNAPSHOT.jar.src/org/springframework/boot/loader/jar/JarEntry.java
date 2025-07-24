/*     */ package org.springframework.boot.loader.jar;
/*     */ 
/*     */ import java.io.IOException;
/*     */ import java.net.MalformedURLException;
/*     */ import java.net.URL;
/*     */ import java.security.CodeSigner;
/*     */ import java.security.cert.Certificate;
/*     */ import java.util.jar.Attributes;
/*     */ import java.util.jar.JarEntry;
/*     */ import java.util.jar.Manifest;
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ class JarEntry
/*     */   extends JarEntry
/*     */   implements FileHeader
/*     */ {
/*     */   private final AsciiBytes name;
/*     */   private Certificate[] certificates;
/*     */   private CodeSigner[] codeSigners;
/*     */   private final JarFile jarFile;
/*     */   private long localHeaderOffset;
/*     */   
/*     */   JarEntry(JarFile jarFile, CentralDirectoryFileHeader header) {
/*  46 */     super(header.getName().toString());
/*  47 */     this.name = header.getName();
/*  48 */     this.jarFile = jarFile;
/*  49 */     this.localHeaderOffset = header.getLocalHeaderOffset();
/*  50 */     setCompressedSize(header.getCompressedSize());
/*  51 */     setMethod(header.getMethod());
/*  52 */     setCrc(header.getCrc());
/*  53 */     setComment(header.getComment().toString());
/*  54 */     setSize(header.getSize());
/*  55 */     setTime(header.getTime());
/*  56 */     setExtra(header.getExtra());
/*     */   }
/*     */   
/*     */   AsciiBytes getAsciiBytesName() {
/*  60 */     return this.name;
/*     */   }
/*     */ 
/*     */   
/*     */   public boolean hasName(CharSequence name, char suffix) {
/*  65 */     return this.name.matches(name, suffix);
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   URL getUrl() throws MalformedURLException {
/*  74 */     return new URL(this.jarFile.getUrl(), getName());
/*     */   }
/*     */ 
/*     */   
/*     */   public Attributes getAttributes() throws IOException {
/*  79 */     Manifest manifest = this.jarFile.getManifest();
/*  80 */     return (manifest != null) ? manifest.getAttributes(getName()) : null;
/*     */   }
/*     */ 
/*     */   
/*     */   public Certificate[] getCertificates() {
/*  85 */     if (this.jarFile.isSigned() && this.certificates == null) {
/*  86 */       this.jarFile.setupEntryCertificates(this);
/*     */     }
/*  88 */     return this.certificates;
/*     */   }
/*     */ 
/*     */   
/*     */   public CodeSigner[] getCodeSigners() {
/*  93 */     if (this.jarFile.isSigned() && this.codeSigners == null) {
/*  94 */       this.jarFile.setupEntryCertificates(this);
/*     */     }
/*  96 */     return this.codeSigners;
/*     */   }
/*     */   
/*     */   void setCertificates(JarEntry entry) {
/* 100 */     this.certificates = entry.getCertificates();
/* 101 */     this.codeSigners = entry.getCodeSigners();
/*     */   }
/*     */ 
/*     */   
/*     */   public long getLocalHeaderOffset() {
/* 106 */     return this.localHeaderOffset;
/*     */   }
/*     */ }


/* Location:              C:\Users\zhr_0\Desktop\challenge-0.0.1-SNAPSHOT.jar!\org\springframework\boot\loader\jar\JarEntry.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */