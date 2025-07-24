/*     */ package org.springframework.boot.loader.jar;
/*     */ 
/*     */ import java.io.File;
/*     */ import java.io.IOException;
/*     */ import java.lang.ref.SoftReference;
/*     */ import java.lang.reflect.Method;
/*     */ import java.net.MalformedURLException;
/*     */ import java.net.URL;
/*     */ import java.net.URLConnection;
/*     */ import java.net.URLDecoder;
/*     */ import java.net.URLStreamHandler;
/*     */ import java.util.Map;
/*     */ import java.util.concurrent.ConcurrentHashMap;
/*     */ import java.util.logging.Level;
/*     */ import java.util.logging.Logger;
/*     */ import java.util.regex.Pattern;
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
/*     */ public class Handler
/*     */   extends URLStreamHandler
/*     */ {
/*     */   private static final String JAR_PROTOCOL = "jar:";
/*     */   private static final String FILE_PROTOCOL = "file:";
/*     */   private static final String SEPARATOR = "!/";
/*     */   private static final String CURRENT_DIR = "/./";
/*  54 */   private static final Pattern CURRENT_DIR_PATTERN = Pattern.compile("/./");
/*     */   
/*     */   private static final String PARENT_DIR = "/../";
/*     */   
/*  58 */   private static final String[] FALLBACK_HANDLERS = new String[] { "sun.net.www.protocol.jar.Handler" };
/*     */   
/*     */   private static final Method OPEN_CONNECTION_METHOD;
/*     */ 
/*     */   
/*     */   static {
/*  64 */     Method method = null;
/*     */     try {
/*  66 */       method = URLStreamHandler.class.getDeclaredMethod("openConnection", new Class[] { URL.class });
/*     */     
/*     */     }
/*  69 */     catch (Exception exception) {}
/*     */ 
/*     */     
/*  72 */     OPEN_CONNECTION_METHOD = method;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*  78 */   private static SoftReference<Map<File, JarFile>> rootFileCache = new SoftReference<>(null);
/*     */   
/*     */   private final JarFile jarFile;
/*     */   
/*     */   private URLStreamHandler fallbackHandler;
/*     */ 
/*     */   
/*     */   public Handler() {
/*  86 */     this(null);
/*     */   }
/*     */   
/*     */   public Handler(JarFile jarFile) {
/*  90 */     this.jarFile = jarFile;
/*     */   }
/*     */ 
/*     */   
/*     */   protected URLConnection openConnection(URL url) throws IOException {
/*  95 */     if (this.jarFile != null && isUrlInJarFile(url, this.jarFile)) {
/*  96 */       return JarURLConnection.get(url, this.jarFile);
/*     */     }
/*     */     try {
/*  99 */       return JarURLConnection.get(url, getRootJarFileFromUrl(url));
/*     */     }
/* 101 */     catch (Exception ex) {
/* 102 */       return openFallbackConnection(url, ex);
/*     */     } 
/*     */   }
/*     */ 
/*     */ 
/*     */   
/*     */   private boolean isUrlInJarFile(URL url, JarFile jarFile) throws MalformedURLException {
/* 109 */     return (url.getPath().startsWith(jarFile.getUrl().getPath()) && url
/* 110 */       .toString().startsWith(jarFile.getUrlString()));
/*     */   }
/*     */ 
/*     */   
/*     */   private URLConnection openFallbackConnection(URL url, Exception reason) throws IOException {
/*     */     try {
/* 116 */       return openConnection(getFallbackHandler(), url);
/*     */     }
/* 118 */     catch (Exception ex) {
/* 119 */       if (reason instanceof IOException) {
/* 120 */         log(false, "Unable to open fallback handler", ex);
/* 121 */         throw (IOException)reason;
/*     */       } 
/* 123 */       log(true, "Unable to open fallback handler", ex);
/* 124 */       if (reason instanceof RuntimeException) {
/* 125 */         throw (RuntimeException)reason;
/*     */       }
/* 127 */       throw new IllegalStateException(reason);
/*     */     } 
/*     */   }
/*     */   
/*     */   private void log(boolean warning, String message, Exception cause) {
/*     */     try {
/* 133 */       Level level = warning ? Level.WARNING : Level.FINEST;
/* 134 */       Logger.getLogger(getClass().getName()).log(level, message, cause);
/*     */     }
/* 136 */     catch (Exception ex) {
/* 137 */       if (warning) {
/* 138 */         System.err.println("WARNING: " + message);
/*     */       }
/*     */     } 
/*     */   }
/*     */   
/*     */   private URLStreamHandler getFallbackHandler() {
/* 144 */     if (this.fallbackHandler != null) {
/* 145 */       return this.fallbackHandler;
/*     */     }
/* 147 */     for (String handlerClassName : FALLBACK_HANDLERS) {
/*     */       try {
/* 149 */         Class<?> handlerClass = Class.forName(handlerClassName);
/* 150 */         this.fallbackHandler = (URLStreamHandler)handlerClass.newInstance();
/* 151 */         return this.fallbackHandler;
/*     */       }
/* 153 */       catch (Exception exception) {}
/*     */     } 
/*     */ 
/*     */     
/* 157 */     throw new IllegalStateException("Unable to find fallback handler");
/*     */   }
/*     */ 
/*     */   
/*     */   private URLConnection openConnection(URLStreamHandler handler, URL url) throws Exception {
/* 162 */     if (OPEN_CONNECTION_METHOD == null) {
/* 163 */       throw new IllegalStateException("Unable to invoke fallback open connection method");
/*     */     }
/*     */     
/* 166 */     OPEN_CONNECTION_METHOD.setAccessible(true);
/* 167 */     return (URLConnection)OPEN_CONNECTION_METHOD.invoke(handler, new Object[] { url });
/*     */   }
/*     */ 
/*     */   
/*     */   protected void parseURL(URL context, String spec, int start, int limit) {
/* 172 */     if (spec.regionMatches(true, 0, "jar:", 0, "jar:".length())) {
/* 173 */       setFile(context, getFileFromSpec(spec.substring(start, limit)));
/*     */     } else {
/*     */       
/* 176 */       setFile(context, getFileFromContext(context, spec.substring(start, limit)));
/*     */     } 
/*     */   }
/*     */   
/*     */   private String getFileFromSpec(String spec) {
/* 181 */     int separatorIndex = spec.lastIndexOf("!/");
/* 182 */     if (separatorIndex == -1) {
/* 183 */       throw new IllegalArgumentException("No !/ in spec '" + spec + "'");
/*     */     }
/*     */     try {
/* 186 */       new URL(spec.substring(0, separatorIndex));
/* 187 */       return spec;
/*     */     }
/* 189 */     catch (MalformedURLException ex) {
/* 190 */       throw new IllegalArgumentException("Invalid spec URL '" + spec + "'", ex);
/*     */     } 
/*     */   }
/*     */   
/*     */   private String getFileFromContext(URL context, String spec) {
/* 195 */     String file = context.getFile();
/* 196 */     if (spec.startsWith("/")) {
/* 197 */       return trimToJarRoot(file) + "!/" + spec.substring(1);
/*     */     }
/* 199 */     if (file.endsWith("/")) {
/* 200 */       return file + spec;
/*     */     }
/* 202 */     int lastSlashIndex = file.lastIndexOf('/');
/* 203 */     if (lastSlashIndex == -1) {
/* 204 */       throw new IllegalArgumentException("No / found in context URL's file '" + file + "'");
/*     */     }
/*     */     
/* 207 */     return file.substring(0, lastSlashIndex + 1) + spec;
/*     */   }
/*     */   
/*     */   private String trimToJarRoot(String file) {
/* 211 */     int lastSeparatorIndex = file.lastIndexOf("!/");
/* 212 */     if (lastSeparatorIndex == -1) {
/* 213 */       throw new IllegalArgumentException("No !/ found in context URL's file '" + file + "'");
/*     */     }
/*     */     
/* 216 */     return file.substring(0, lastSeparatorIndex);
/*     */   }
/*     */   
/*     */   private void setFile(URL context, String file) {
/* 220 */     String path = normalize(file);
/* 221 */     String query = null;
/* 222 */     int queryIndex = path.lastIndexOf('?');
/* 223 */     if (queryIndex != -1) {
/* 224 */       query = path.substring(queryIndex + 1);
/* 225 */       path = path.substring(0, queryIndex);
/*     */     } 
/* 227 */     setURL(context, "jar:", null, -1, null, null, path, query, context
/* 228 */         .getRef());
/*     */   }
/*     */   
/*     */   private String normalize(String file) {
/* 232 */     if (!file.contains("/./") && !file.contains("/../")) {
/* 233 */       return file;
/*     */     }
/* 235 */     int afterLastSeparatorIndex = file.lastIndexOf("!/") + "!/".length();
/* 236 */     String afterSeparator = file.substring(afterLastSeparatorIndex);
/* 237 */     afterSeparator = replaceParentDir(afterSeparator);
/* 238 */     afterSeparator = replaceCurrentDir(afterSeparator);
/* 239 */     return file.substring(0, afterLastSeparatorIndex) + afterSeparator;
/*     */   }
/*     */   
/*     */   private String replaceParentDir(String file) {
/*     */     int parentDirIndex;
/* 244 */     while ((parentDirIndex = file.indexOf("/../")) >= 0) {
/* 245 */       int precedingSlashIndex = file.lastIndexOf('/', parentDirIndex - 1);
/* 246 */       if (precedingSlashIndex >= 0) {
/*     */         
/* 248 */         file = file.substring(0, precedingSlashIndex) + file.substring(parentDirIndex + 3);
/*     */         continue;
/*     */       } 
/* 251 */       file = file.substring(parentDirIndex + 4);
/*     */     } 
/*     */     
/* 254 */     return file;
/*     */   }
/*     */   
/*     */   private String replaceCurrentDir(String file) {
/* 258 */     return CURRENT_DIR_PATTERN.matcher(file).replaceAll("/");
/*     */   }
/*     */ 
/*     */   
/*     */   protected int hashCode(URL u) {
/* 263 */     return hashCode(u.getProtocol(), u.getFile());
/*     */   }
/*     */   
/*     */   private int hashCode(String protocol, String file) {
/* 267 */     int result = (protocol != null) ? protocol.hashCode() : 0;
/* 268 */     int separatorIndex = file.indexOf("!/");
/* 269 */     if (separatorIndex == -1) {
/* 270 */       return result + file.hashCode();
/*     */     }
/* 272 */     String source = file.substring(0, separatorIndex);
/* 273 */     String entry = canonicalize(file.substring(separatorIndex + 2));
/*     */     try {
/* 275 */       result += (new URL(source)).hashCode();
/*     */     }
/* 277 */     catch (MalformedURLException ex) {
/* 278 */       result += source.hashCode();
/*     */     } 
/* 280 */     result += entry.hashCode();
/* 281 */     return result;
/*     */   }
/*     */ 
/*     */   
/*     */   protected boolean sameFile(URL u1, URL u2) {
/* 286 */     if (!u1.getProtocol().equals("jar") || !u2.getProtocol().equals("jar")) {
/* 287 */       return false;
/*     */     }
/* 289 */     int separator1 = u1.getFile().indexOf("!/");
/* 290 */     int separator2 = u2.getFile().indexOf("!/");
/* 291 */     if (separator1 == -1 || separator2 == -1) {
/* 292 */       return super.sameFile(u1, u2);
/*     */     }
/* 294 */     String nested1 = u1.getFile().substring(separator1 + "!/".length());
/* 295 */     String nested2 = u2.getFile().substring(separator2 + "!/".length());
/* 296 */     if (!nested1.equals(nested2)) {
/* 297 */       String canonical1 = canonicalize(nested1);
/* 298 */       String canonical2 = canonicalize(nested2);
/* 299 */       if (!canonical1.equals(canonical2)) {
/* 300 */         return false;
/*     */       }
/*     */     } 
/* 303 */     String root1 = u1.getFile().substring(0, separator1);
/* 304 */     String root2 = u2.getFile().substring(0, separator2);
/*     */     try {
/* 306 */       return super.sameFile(new URL(root1), new URL(root2));
/*     */     }
/* 308 */     catch (MalformedURLException malformedURLException) {
/*     */ 
/*     */       
/* 311 */       return super.sameFile(u1, u2);
/*     */     } 
/*     */   }
/*     */   private String canonicalize(String path) {
/* 315 */     return path.replace("!/", "/");
/*     */   }
/*     */   
/*     */   public JarFile getRootJarFileFromUrl(URL url) throws IOException {
/* 319 */     String spec = url.getFile();
/* 320 */     int separatorIndex = spec.indexOf("!/");
/* 321 */     if (separatorIndex == -1) {
/* 322 */       throw new MalformedURLException("Jar URL does not contain !/ separator");
/*     */     }
/* 324 */     String name = spec.substring(0, separatorIndex);
/* 325 */     return getRootJarFile(name);
/*     */   }
/*     */   
/*     */   private JarFile getRootJarFile(String name) throws IOException {
/*     */     try {
/* 330 */       if (!name.startsWith("file:")) {
/* 331 */         throw new IllegalStateException("Not a file URL");
/*     */       }
/* 333 */       String path = name.substring("file:".length());
/* 334 */       File file = new File(URLDecoder.decode(path, "UTF-8"));
/* 335 */       Map<File, JarFile> cache = rootFileCache.get();
/* 336 */       JarFile result = (cache != null) ? cache.get(file) : null;
/* 337 */       if (result == null) {
/* 338 */         result = new JarFile(file);
/* 339 */         addToRootFileCache(file, result);
/*     */       } 
/* 341 */       return result;
/*     */     }
/* 343 */     catch (Exception ex) {
/* 344 */       throw new IOException("Unable to open root Jar file '" + name + "'", ex);
/*     */     } 
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   static void addToRootFileCache(File sourceFile, JarFile jarFile) {
/* 354 */     Map<File, JarFile> cache = rootFileCache.get();
/* 355 */     if (cache == null) {
/* 356 */       cache = new ConcurrentHashMap<>();
/* 357 */       rootFileCache = new SoftReference<>(cache);
/*     */     } 
/* 359 */     cache.put(sourceFile, jarFile);
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public static void setUseFastConnectionExceptions(boolean useFastConnectionExceptions) {
/* 370 */     JarURLConnection.setUseFastExceptions(useFastConnectionExceptions);
/*     */   }
/*     */ }


/* Location:              C:\Users\zhr_0\Desktop\challenge-0.0.1-SNAPSHOT.jar!\org\springframework\boot\loader\jar\Handler.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */