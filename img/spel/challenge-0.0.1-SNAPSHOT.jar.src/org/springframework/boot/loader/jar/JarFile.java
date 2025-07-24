/*     */ package org.springframework.boot.loader.jar;
/*     */ 
/*     */ import java.io.File;
/*     */ import java.io.IOException;
/*     */ import java.io.InputStream;
/*     */ import java.lang.ref.SoftReference;
/*     */ import java.net.MalformedURLException;
/*     */ import java.net.URL;
/*     */ import java.util.Enumeration;
/*     */ import java.util.Iterator;
/*     */ import java.util.function.Supplier;
/*     */ import java.util.jar.JarEntry;
/*     */ import java.util.jar.JarFile;
/*     */ import java.util.jar.JarInputStream;
/*     */ import java.util.jar.Manifest;
/*     */ import java.util.zip.ZipEntry;
/*     */ import org.springframework.boot.loader.data.RandomAccessData;
/*     */ import org.springframework.boot.loader.data.RandomAccessDataFile;
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
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ public class JarFile
/*     */   extends JarFile
/*     */ {
/*     */   private static final String MANIFEST_NAME = "META-INF/MANIFEST.MF";
/*     */   private static final String PROTOCOL_HANDLER = "java.protocol.handler.pkgs";
/*     */   private static final String HANDLERS_PACKAGE = "org.springframework.boot.loader";
/*  58 */   private static final AsciiBytes META_INF = new AsciiBytes("META-INF/");
/*     */   
/*  60 */   private static final AsciiBytes SIGNATURE_FILE_EXTENSION = new AsciiBytes(".SF");
/*     */ 
/*     */   
/*     */   private final RandomAccessDataFile rootFile;
/*     */ 
/*     */   
/*     */   private final String pathFromRoot;
/*     */ 
/*     */   
/*     */   private final RandomAccessData data;
/*     */ 
/*     */   
/*     */   private final JarFileType type;
/*     */   
/*     */   private URL url;
/*     */   
/*     */   private String urlString;
/*     */   
/*     */   private JarFileEntries entries;
/*     */   
/*     */   private Supplier<Manifest> manifestSupplier;
/*     */   
/*     */   private SoftReference<Manifest> manifest;
/*     */   
/*     */   private boolean signed;
/*     */ 
/*     */   
/*     */   public JarFile(File file) throws IOException {
/*  88 */     this(new RandomAccessDataFile(file));
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   JarFile(RandomAccessDataFile file) throws IOException {
/*  97 */     this(file, "", (RandomAccessData)file, JarFileType.DIRECT);
/*     */   }
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
/*     */   private JarFile(RandomAccessDataFile rootFile, String pathFromRoot, RandomAccessData data, JarFileType type) throws IOException {
/* 111 */     this(rootFile, pathFromRoot, data, null, type, null);
/*     */   }
/*     */ 
/*     */ 
/*     */   
/*     */   private JarFile(RandomAccessDataFile rootFile, String pathFromRoot, RandomAccessData data, JarEntryFilter filter, JarFileType type, Supplier<Manifest> manifestSupplier) throws IOException {
/* 117 */     super(rootFile.getFile());
/* 118 */     this.rootFile = rootFile;
/* 119 */     this.pathFromRoot = pathFromRoot;
/* 120 */     CentralDirectoryParser parser = new CentralDirectoryParser();
/* 121 */     this.entries = parser.<JarFileEntries>addVisitor(new JarFileEntries(this, filter));
/* 122 */     parser.addVisitor(centralDirectoryVisitor());
/* 123 */     this.data = parser.parse(data, (filter == null));
/* 124 */     this.type = type;
/* 125 */     this.manifestSupplier = (manifestSupplier != null) ? manifestSupplier : (() -> {
/*     */         try (InputStream inputStream = getInputStream("META-INF/MANIFEST.MF")) {
/*     */           if (inputStream == null) {
/*     */             return null;
/*     */           }
/*     */           
/*     */           return new Manifest(inputStream);
/* 132 */         } catch (IOException ex) {
/*     */           throw new RuntimeException(ex);
/*     */         } 
/*     */       });
/*     */   }
/*     */   
/*     */   private CentralDirectoryVisitor centralDirectoryVisitor() {
/* 139 */     return new CentralDirectoryVisitor()
/*     */       {
/*     */         public void visitStart(CentralDirectoryEndRecord endRecord, RandomAccessData centralDirectoryData) {}
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */         
/*     */         public void visitFileHeader(CentralDirectoryFileHeader fileHeader, int dataOffset) {
/* 149 */           AsciiBytes name = fileHeader.getName();
/* 150 */           if (name.startsWith(JarFile.META_INF) && name
/* 151 */             .endsWith(JarFile.SIGNATURE_FILE_EXTENSION)) {
/* 152 */             JarFile.this.signed = true;
/*     */           }
/*     */         }
/*     */ 
/*     */ 
/*     */         
/*     */         public void visitEnd() {}
/*     */       };
/*     */   }
/*     */ 
/*     */   
/*     */   protected final RandomAccessDataFile getRootJarFile() {
/* 164 */     return this.rootFile;
/*     */   }
/*     */   
/*     */   RandomAccessData getData() {
/* 168 */     return this.data;
/*     */   }
/*     */ 
/*     */   
/*     */   public Manifest getManifest() throws IOException {
/* 173 */     Manifest manifest = (this.manifest != null) ? this.manifest.get() : null;
/* 174 */     if (manifest == null) {
/*     */       try {
/* 176 */         manifest = this.manifestSupplier.get();
/*     */       }
/* 178 */       catch (RuntimeException ex) {
/* 179 */         throw new IOException(ex);
/*     */       } 
/* 181 */       this.manifest = new SoftReference<>(manifest);
/*     */     } 
/* 183 */     return manifest;
/*     */   }
/*     */ 
/*     */   
/*     */   public Enumeration<JarEntry> entries() {
/* 188 */     final Iterator<JarEntry> iterator = this.entries.iterator();
/* 189 */     return new Enumeration<JarEntry>()
/*     */       {
/*     */         public boolean hasMoreElements()
/*     */         {
/* 193 */           return iterator.hasNext();
/*     */         }
/*     */ 
/*     */         
/*     */         public JarEntry nextElement() {
/* 198 */           return iterator.next();
/*     */         }
/*     */       };
/*     */   }
/*     */ 
/*     */   
/*     */   public JarEntry getJarEntry(CharSequence name) {
/* 205 */     return this.entries.getEntry(name);
/*     */   }
/*     */ 
/*     */   
/*     */   public JarEntry getJarEntry(String name) {
/* 210 */     return (JarEntry)getEntry(name);
/*     */   }
/*     */   
/*     */   public boolean containsEntry(String name) {
/* 214 */     return this.entries.containsEntry(name);
/*     */   }
/*     */ 
/*     */   
/*     */   public ZipEntry getEntry(String name) {
/* 219 */     return this.entries.getEntry(name);
/*     */   }
/*     */ 
/*     */   
/*     */   public synchronized InputStream getInputStream(ZipEntry entry) throws IOException {
/* 224 */     if (entry instanceof JarEntry) {
/* 225 */       return this.entries.getInputStream((JarEntry)entry);
/*     */     }
/* 227 */     return getInputStream((entry != null) ? entry.getName() : null);
/*     */   }
/*     */   
/*     */   InputStream getInputStream(String name) throws IOException {
/* 231 */     return this.entries.getInputStream(name);
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public synchronized JarFile getNestedJarFile(ZipEntry entry) throws IOException {
/* 241 */     return getNestedJarFile((JarEntry)entry);
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public synchronized JarFile getNestedJarFile(JarEntry entry) throws IOException {
/*     */     try {
/* 252 */       return createJarFileFromEntry(entry);
/*     */     }
/* 254 */     catch (Exception ex) {
/* 255 */       throw new IOException("Unable to open nested jar file '" + entry
/* 256 */           .getName() + "'", ex);
/*     */     } 
/*     */   }
/*     */   
/*     */   private JarFile createJarFileFromEntry(JarEntry entry) throws IOException {
/* 261 */     if (entry.isDirectory()) {
/* 262 */       return createJarFileFromDirectoryEntry(entry);
/*     */     }
/* 264 */     return createJarFileFromFileEntry(entry);
/*     */   }
/*     */   
/*     */   private JarFile createJarFileFromDirectoryEntry(JarEntry entry) throws IOException {
/* 268 */     AsciiBytes name = entry.getAsciiBytesName();
/* 269 */     JarEntryFilter filter = candidate -> 
/* 270 */       (candidate.startsWith(name) && !candidate.equals(name)) ? candidate.substring(name.length()) : null;
/*     */ 
/*     */ 
/*     */ 
/*     */     
/* 275 */     return new JarFile(this.rootFile, this.pathFromRoot + "!/" + entry
/*     */         
/* 277 */         .getName().substring(0, name.length() - 1), this.data, filter, JarFileType.NESTED_DIRECTORY, this.manifestSupplier);
/*     */   }
/*     */ 
/*     */   
/*     */   private JarFile createJarFileFromFileEntry(JarEntry entry) throws IOException {
/* 282 */     if (entry.getMethod() != 0) {
/* 283 */       throw new IllegalStateException("Unable to open nested entry '" + entry
/* 284 */           .getName() + "'. It has been compressed and nested jar files must be stored without compression. Please check the mechanism used to create your executable jar file");
/*     */     }
/*     */ 
/*     */     
/* 288 */     RandomAccessData entryData = this.entries.getEntryData(entry.getName());
/* 289 */     return new JarFile(this.rootFile, this.pathFromRoot + "!/" + entry.getName(), entryData, JarFileType.NESTED_JAR);
/*     */   }
/*     */ 
/*     */ 
/*     */   
/*     */   public int size() {
/* 295 */     return this.entries.getSize();
/*     */   }
/*     */ 
/*     */   
/*     */   public void close() throws IOException {
/* 300 */     super.close();
/* 301 */     if (this.type == JarFileType.DIRECT) {
/* 302 */       this.rootFile.close();
/*     */     }
/*     */   }
/*     */   
/*     */   String getUrlString() throws MalformedURLException {
/* 307 */     if (this.urlString == null) {
/* 308 */       this.urlString = getUrl().toString();
/*     */     }
/* 310 */     return this.urlString;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public URL getUrl() throws MalformedURLException {
/* 320 */     if (this.url == null) {
/* 321 */       Handler handler = new Handler(this);
/* 322 */       String file = this.rootFile.getFile().toURI() + this.pathFromRoot + "!/";
/* 323 */       file = file.replace("file:////", "file://");
/* 324 */       this.url = new URL("jar", "", -1, file, handler);
/*     */     } 
/* 326 */     return this.url;
/*     */   }
/*     */ 
/*     */   
/*     */   public String toString() {
/* 331 */     return getName();
/*     */   }
/*     */ 
/*     */   
/*     */   public String getName() {
/* 336 */     return this.rootFile.getFile() + this.pathFromRoot;
/*     */   }
/*     */   
/*     */   boolean isSigned() {
/* 340 */     return this.signed;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   void setupEntryCertificates(JarEntry entry) {
/* 347 */     try (JarInputStream inputStream = new JarInputStream(
/* 348 */           getData().getInputStream())) {
/* 349 */       JarEntry certEntry = inputStream.getNextJarEntry();
/* 350 */       while (certEntry != null) {
/* 351 */         inputStream.closeEntry();
/* 352 */         if (entry.getName().equals(certEntry.getName())) {
/* 353 */           setCertificates(entry, certEntry);
/*     */         }
/* 355 */         setCertificates(getJarEntry(certEntry.getName()), certEntry);
/* 356 */         certEntry = inputStream.getNextJarEntry();
/*     */       }
/*     */     
/*     */     }
/* 360 */     catch (IOException ex) {
/* 361 */       throw new IllegalStateException(ex);
/*     */     } 
/*     */   }
/*     */   
/*     */   private void setCertificates(JarEntry entry, JarEntry certEntry) {
/* 366 */     if (entry != null) {
/* 367 */       entry.setCertificates(certEntry);
/*     */     }
/*     */   }
/*     */   
/*     */   public void clearCache() {
/* 372 */     this.entries.clearCache();
/*     */   }
/*     */   
/*     */   protected String getPathFromRoot() {
/* 376 */     return this.pathFromRoot;
/*     */   }
/*     */   
/*     */   JarFileType getType() {
/* 380 */     return this.type;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public static void registerUrlProtocolHandler() {
/* 388 */     String handlers = System.getProperty("java.protocol.handler.pkgs", "");
/* 389 */     System.setProperty("java.protocol.handler.pkgs", "".equals(handlers) ? "org.springframework.boot.loader" : (handlers + "|" + "org.springframework.boot.loader"));
/*     */     
/* 391 */     resetCachedUrlHandlers();
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   private static void resetCachedUrlHandlers() {
/*     */     try {
/* 401 */       URL.setURLStreamHandlerFactory(null);
/*     */     }
/* 403 */     catch (Error error) {}
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   enum JarFileType
/*     */   {
/* 413 */     DIRECT, NESTED_DIRECTORY, NESTED_JAR;
/*     */   }
/*     */ }


/* Location:              C:\Users\zhr_0\Desktop\challenge-0.0.1-SNAPSHOT.jar!\org\springframework\boot\loader\jar\JarFile.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */