/*     */ package org.springframework.boot.loader.jar;
/*     */ 
/*     */ import java.io.ByteArrayOutputStream;
/*     */ import java.io.FileNotFoundException;
/*     */ import java.io.FilePermission;
/*     */ import java.io.IOException;
/*     */ import java.io.InputStream;
/*     */ import java.io.UnsupportedEncodingException;
/*     */ import java.net.JarURLConnection;
/*     */ import java.net.MalformedURLException;
/*     */ import java.net.URL;
/*     */ import java.net.URLConnection;
/*     */ import java.net.URLEncoder;
/*     */ import java.net.URLStreamHandler;
/*     */ import java.security.Permission;
/*     */ import java.util.jar.JarEntry;
/*     */ import java.util.jar.JarFile;
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
/*     */ final class JarURLConnection
/*     */   extends JarURLConnection
/*     */ {
/*  41 */   private static ThreadLocal<Boolean> useFastExceptions = new ThreadLocal<>();
/*     */   
/*  43 */   private static final FileNotFoundException FILE_NOT_FOUND_EXCEPTION = new FileNotFoundException("Jar file or entry not found");
/*     */ 
/*     */   
/*  46 */   private static final IllegalStateException NOT_FOUND_CONNECTION_EXCEPTION = new IllegalStateException(FILE_NOT_FOUND_EXCEPTION);
/*     */   
/*     */   private static final String SEPARATOR = "!/";
/*     */   
/*     */   private static final URL EMPTY_JAR_URL;
/*     */ 
/*     */   
/*     */   static {
/*     */     try {
/*  55 */       EMPTY_JAR_URL = new URL("jar:", null, 0, "file:!/", new URLStreamHandler()
/*     */           {
/*     */             
/*     */             protected URLConnection openConnection(URL u) throws IOException
/*     */             {
/*  60 */               return null;
/*     */             }
/*     */           });
/*     */     }
/*  64 */     catch (MalformedURLException ex) {
/*  65 */       throw new IllegalStateException(ex);
/*     */     } 
/*     */   }
/*     */   
/*  69 */   private static final JarEntryName EMPTY_JAR_ENTRY_NAME = new JarEntryName(new StringSequence(""));
/*     */ 
/*     */   
/*     */   private static final String READ_ACTION = "read";
/*     */ 
/*     */   
/*  75 */   private static final JarURLConnection NOT_FOUND_CONNECTION = notFound();
/*     */ 
/*     */   
/*     */   private final JarFile jarFile;
/*     */   
/*     */   private Permission permission;
/*     */   
/*     */   private URL jarFileUrl;
/*     */   
/*     */   private final JarEntryName jarEntryName;
/*     */   
/*     */   private JarEntry jarEntry;
/*     */ 
/*     */   
/*     */   private JarURLConnection(URL url, JarFile jarFile, JarEntryName jarEntryName) throws IOException {
/*  90 */     super(EMPTY_JAR_URL);
/*  91 */     this.url = url;
/*  92 */     this.jarFile = jarFile;
/*  93 */     this.jarEntryName = jarEntryName;
/*     */   }
/*     */ 
/*     */   
/*     */   public void connect() throws IOException {
/*  98 */     if (this.jarFile == null) {
/*  99 */       throw FILE_NOT_FOUND_EXCEPTION;
/*     */     }
/* 101 */     if (!this.jarEntryName.isEmpty() && this.jarEntry == null) {
/* 102 */       this.jarEntry = this.jarFile.getJarEntry(getEntryName());
/* 103 */       if (this.jarEntry == null) {
/* 104 */         throwFileNotFound(this.jarEntryName, this.jarFile);
/*     */       }
/*     */     } 
/* 107 */     this.connected = true;
/*     */   }
/*     */ 
/*     */   
/*     */   public JarFile getJarFile() throws IOException {
/* 112 */     connect();
/* 113 */     return this.jarFile;
/*     */   }
/*     */ 
/*     */   
/*     */   public URL getJarFileURL() {
/* 118 */     if (this.jarFile == null) {
/* 119 */       throw NOT_FOUND_CONNECTION_EXCEPTION;
/*     */     }
/* 121 */     if (this.jarFileUrl == null) {
/* 122 */       this.jarFileUrl = buildJarFileUrl();
/*     */     }
/* 124 */     return this.jarFileUrl;
/*     */   }
/*     */   
/*     */   private URL buildJarFileUrl() {
/*     */     try {
/* 129 */       String spec = this.jarFile.getUrl().getFile();
/* 130 */       if (spec.endsWith("!/")) {
/* 131 */         spec = spec.substring(0, spec.length() - "!/".length());
/*     */       }
/* 133 */       if (spec.indexOf("!/") == -1) {
/* 134 */         return new URL(spec);
/*     */       }
/* 136 */       return new URL("jar:" + spec);
/*     */     }
/* 138 */     catch (MalformedURLException ex) {
/* 139 */       throw new IllegalStateException(ex);
/*     */     } 
/*     */   }
/*     */ 
/*     */   
/*     */   public JarEntry getJarEntry() throws IOException {
/* 145 */     if (this.jarEntryName == null || this.jarEntryName.isEmpty()) {
/* 146 */       return null;
/*     */     }
/* 148 */     connect();
/* 149 */     return this.jarEntry;
/*     */   }
/*     */ 
/*     */   
/*     */   public String getEntryName() {
/* 154 */     if (this.jarFile == null) {
/* 155 */       throw NOT_FOUND_CONNECTION_EXCEPTION;
/*     */     }
/* 157 */     return this.jarEntryName.toString();
/*     */   }
/*     */ 
/*     */   
/*     */   public InputStream getInputStream() throws IOException {
/* 162 */     if (this.jarFile == null) {
/* 163 */       throw FILE_NOT_FOUND_EXCEPTION;
/*     */     }
/* 165 */     if (this.jarEntryName.isEmpty() && this.jarFile
/* 166 */       .getType() == JarFile.JarFileType.DIRECT) {
/* 167 */       throw new IOException("no entry name specified");
/*     */     }
/* 169 */     connect();
/*     */ 
/*     */     
/* 172 */     InputStream inputStream = this.jarEntryName.isEmpty() ? this.jarFile.getData().getInputStream() : this.jarFile.getInputStream(this.jarEntry);
/* 173 */     if (inputStream == null) {
/* 174 */       throwFileNotFound(this.jarEntryName, this.jarFile);
/*     */     }
/* 176 */     return inputStream;
/*     */   }
/*     */ 
/*     */   
/*     */   private void throwFileNotFound(Object entry, JarFile jarFile) throws FileNotFoundException {
/* 181 */     if (Boolean.TRUE.equals(useFastExceptions.get())) {
/* 182 */       throw FILE_NOT_FOUND_EXCEPTION;
/*     */     }
/* 184 */     throw new FileNotFoundException("JAR entry " + entry + " not found in " + jarFile
/* 185 */         .getName());
/*     */   }
/*     */ 
/*     */   
/*     */   public int getContentLength() {
/* 190 */     long length = getContentLengthLong();
/* 191 */     if (length > 2147483647L) {
/* 192 */       return -1;
/*     */     }
/* 194 */     return (int)length;
/*     */   }
/*     */ 
/*     */   
/*     */   public long getContentLengthLong() {
/* 199 */     if (this.jarFile == null) {
/* 200 */       return -1L;
/*     */     }
/*     */     try {
/* 203 */       if (this.jarEntryName.isEmpty()) {
/* 204 */         return this.jarFile.size();
/*     */       }
/* 206 */       JarEntry entry = getJarEntry();
/* 207 */       return (entry != null) ? (int)entry.getSize() : -1L;
/*     */     }
/* 209 */     catch (IOException ex) {
/* 210 */       return -1L;
/*     */     } 
/*     */   }
/*     */ 
/*     */   
/*     */   public Object getContent() throws IOException {
/* 216 */     connect();
/* 217 */     return this.jarEntryName.isEmpty() ? this.jarFile : super.getContent();
/*     */   }
/*     */ 
/*     */   
/*     */   public String getContentType() {
/* 222 */     return (this.jarEntryName != null) ? this.jarEntryName.getContentType() : null;
/*     */   }
/*     */ 
/*     */   
/*     */   public Permission getPermission() throws IOException {
/* 227 */     if (this.jarFile == null) {
/* 228 */       throw FILE_NOT_FOUND_EXCEPTION;
/*     */     }
/* 230 */     if (this.permission == null) {
/* 231 */       this
/* 232 */         .permission = new FilePermission(this.jarFile.getRootJarFile().getFile().getPath(), "read");
/*     */     }
/* 234 */     return this.permission;
/*     */   }
/*     */ 
/*     */   
/*     */   public long getLastModified() {
/* 239 */     if (this.jarFile == null || this.jarEntryName.isEmpty()) {
/* 240 */       return 0L;
/*     */     }
/*     */     try {
/* 243 */       JarEntry entry = getJarEntry();
/* 244 */       return (entry != null) ? entry.getTime() : 0L;
/*     */     }
/* 246 */     catch (IOException ex) {
/* 247 */       return 0L;
/*     */     } 
/*     */   }
/*     */   
/*     */   static void setUseFastExceptions(boolean useFastExceptions) {
/* 252 */     JarURLConnection.useFastExceptions.set(Boolean.valueOf(useFastExceptions));
/*     */   }
/*     */   
/*     */   static JarURLConnection get(URL url, JarFile jarFile) throws IOException {
/* 256 */     StringSequence spec = new StringSequence(url.getFile());
/* 257 */     int index = indexOfRootSpec(spec, jarFile.getPathFromRoot());
/* 258 */     if (index == -1) {
/* 259 */       return Boolean.TRUE.equals(useFastExceptions.get()) ? NOT_FOUND_CONNECTION : new JarURLConnection(url, null, EMPTY_JAR_ENTRY_NAME);
/*     */     }
/*     */     
/*     */     int separator;
/* 263 */     while ((separator = spec.indexOf("!/", index)) > 0) {
/* 264 */       JarEntryName entryName = JarEntryName.get(spec.subSequence(index, separator));
/* 265 */       JarEntry jarEntry = jarFile.getJarEntry(entryName.toCharSequence());
/* 266 */       if (jarEntry == null) {
/* 267 */         return notFound(jarFile, entryName);
/*     */       }
/* 269 */       jarFile = jarFile.getNestedJarFile(jarEntry);
/* 270 */       index = separator + "!/".length();
/*     */     } 
/* 272 */     JarEntryName jarEntryName = JarEntryName.get(spec, index);
/* 273 */     if (Boolean.TRUE.equals(useFastExceptions.get()) && !jarEntryName.isEmpty() && 
/* 274 */       !jarFile.containsEntry(jarEntryName.toString())) {
/* 275 */       return NOT_FOUND_CONNECTION;
/*     */     }
/* 277 */     return new JarURLConnection(url, jarFile, jarEntryName);
/*     */   }
/*     */   
/*     */   private static int indexOfRootSpec(StringSequence file, String pathFromRoot) {
/* 281 */     int separatorIndex = file.indexOf("!/");
/* 282 */     if (separatorIndex < 0 || !file.startsWith(pathFromRoot, separatorIndex)) {
/* 283 */       return -1;
/*     */     }
/* 285 */     return separatorIndex + "!/".length() + pathFromRoot.length();
/*     */   }
/*     */   
/*     */   private static JarURLConnection notFound() {
/*     */     try {
/* 290 */       return notFound((JarFile)null, (JarEntryName)null);
/*     */     }
/* 292 */     catch (IOException ex) {
/* 293 */       throw new IllegalStateException(ex);
/*     */     } 
/*     */   }
/*     */ 
/*     */   
/*     */   private static JarURLConnection notFound(JarFile jarFile, JarEntryName jarEntryName) throws IOException {
/* 299 */     if (Boolean.TRUE.equals(useFastExceptions.get())) {
/* 300 */       return NOT_FOUND_CONNECTION;
/*     */     }
/* 302 */     return new JarURLConnection(null, jarFile, jarEntryName);
/*     */   }
/*     */ 
/*     */ 
/*     */   
/*     */   static class JarEntryName
/*     */   {
/*     */     private final StringSequence name;
/*     */     
/*     */     private String contentType;
/*     */ 
/*     */     
/*     */     JarEntryName(StringSequence spec) {
/* 315 */       this.name = decode(spec);
/*     */     }
/*     */     
/*     */     private StringSequence decode(StringSequence source) {
/* 319 */       if (source.isEmpty() || source.indexOf('%') < 0) {
/* 320 */         return source;
/*     */       }
/* 322 */       ByteArrayOutputStream bos = new ByteArrayOutputStream(source.length());
/* 323 */       write(source.toString(), bos);
/*     */       
/* 325 */       return new StringSequence(AsciiBytes.toString(bos.toByteArray()));
/*     */     }
/*     */     
/*     */     private void write(String source, ByteArrayOutputStream outputStream) {
/* 329 */       int length = source.length();
/* 330 */       for (int i = 0; i < length; i++) {
/* 331 */         int c = source.charAt(i);
/* 332 */         if (c > 127) {
/*     */           try {
/* 334 */             String encoded = URLEncoder.encode(String.valueOf((char)c), "UTF-8");
/*     */             
/* 336 */             write(encoded, outputStream);
/*     */           }
/* 338 */           catch (UnsupportedEncodingException ex) {
/* 339 */             throw new IllegalStateException(ex);
/*     */           } 
/*     */         } else {
/*     */           
/* 343 */           if (c == 37) {
/* 344 */             if (i + 2 >= length) {
/* 345 */               throw new IllegalArgumentException("Invalid encoded sequence \"" + source
/* 346 */                   .substring(i) + "\"");
/*     */             }
/*     */             
/* 349 */             c = decodeEscapeSequence(source, i);
/* 350 */             i += 2;
/*     */           } 
/* 352 */           outputStream.write(c);
/*     */         } 
/*     */       } 
/*     */     }
/*     */     
/*     */     private char decodeEscapeSequence(String source, int i) {
/* 358 */       int hi = Character.digit(source.charAt(i + 1), 16);
/* 359 */       int lo = Character.digit(source.charAt(i + 2), 16);
/* 360 */       if (hi == -1 || lo == -1) {
/* 361 */         throw new IllegalArgumentException("Invalid encoded sequence \"" + source
/* 362 */             .substring(i) + "\"");
/*     */       }
/* 364 */       return (char)((hi << 4) + lo);
/*     */     }
/*     */     
/*     */     public CharSequence toCharSequence() {
/* 368 */       return this.name;
/*     */     }
/*     */ 
/*     */     
/*     */     public String toString() {
/* 373 */       return this.name.toString();
/*     */     }
/*     */     
/*     */     public boolean isEmpty() {
/* 377 */       return this.name.isEmpty();
/*     */     }
/*     */     
/*     */     public String getContentType() {
/* 381 */       if (this.contentType == null) {
/* 382 */         this.contentType = deduceContentType();
/*     */       }
/* 384 */       return this.contentType;
/*     */     }
/*     */ 
/*     */     
/*     */     private String deduceContentType() {
/* 389 */       String type = isEmpty() ? "x-java/jar" : null;
/* 390 */       type = (type != null) ? type : URLConnection.guessContentTypeFromName(toString());
/* 391 */       type = (type != null) ? type : "content/unknown";
/* 392 */       return type;
/*     */     }
/*     */     
/*     */     public static JarEntryName get(StringSequence spec) {
/* 396 */       return get(spec, 0);
/*     */     }
/*     */     
/*     */     public static JarEntryName get(StringSequence spec, int beginIndex) {
/* 400 */       if (spec.length() <= beginIndex) {
/* 401 */         return JarURLConnection.EMPTY_JAR_ENTRY_NAME;
/*     */       }
/* 403 */       return new JarEntryName(spec.subSequence(beginIndex));
/*     */     }
/*     */   }
/*     */ }


/* Location:              C:\Users\zhr_0\Desktop\challenge-0.0.1-SNAPSHOT.jar!\org\springframework\boot\loader\jar\JarURLConnection.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */