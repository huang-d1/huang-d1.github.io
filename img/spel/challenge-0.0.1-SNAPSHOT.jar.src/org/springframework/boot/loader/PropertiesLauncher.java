/*     */ package org.springframework.boot.loader;
/*     */ 
/*     */ import java.io.File;
/*     */ import java.io.FileInputStream;
/*     */ import java.io.IOException;
/*     */ import java.io.InputStream;
/*     */ import java.io.UnsupportedEncodingException;
/*     */ import java.net.HttpURLConnection;
/*     */ import java.net.URL;
/*     */ import java.net.URLConnection;
/*     */ import java.net.URLDecoder;
/*     */ import java.util.ArrayList;
/*     */ import java.util.Collections;
/*     */ import java.util.LinkedHashSet;
/*     */ import java.util.List;
/*     */ import java.util.Locale;
/*     */ import java.util.Properties;
/*     */ import java.util.Set;
/*     */ import java.util.jar.Manifest;
/*     */ import java.util.regex.Matcher;
/*     */ import java.util.regex.Pattern;
/*     */ import org.springframework.boot.loader.archive.Archive;
/*     */ import org.springframework.boot.loader.archive.ExplodedArchive;
/*     */ import org.springframework.boot.loader.archive.JarFileArchive;
/*     */ import org.springframework.boot.loader.util.SystemPropertyUtils;
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
/*     */ public class PropertiesLauncher
/*     */   extends Launcher
/*     */ {
/*     */   private static final String DEBUG = "loader.debug";
/*     */   public static final String MAIN = "loader.main";
/*     */   public static final String PATH = "loader.path";
/*     */   public static final String HOME = "loader.home";
/*     */   public static final String ARGS = "loader.args";
/*     */   public static final String CONFIG_NAME = "loader.config.name";
/*     */   public static final String CONFIG_LOCATION = "loader.config.location";
/*     */   public static final String SET_SYSTEM_PROPERTIES = "loader.system";
/* 125 */   private static final Pattern WORD_SEPARATOR = Pattern.compile("\\W+");
/*     */   
/* 127 */   private static final String NESTED_ARCHIVE_SEPARATOR = "!" + File.separator;
/*     */   
/*     */   private final File home;
/*     */   
/* 131 */   private List<String> paths = new ArrayList<>();
/*     */   
/* 133 */   private final Properties properties = new Properties();
/*     */   
/*     */   private Archive parent;
/*     */   
/*     */   public PropertiesLauncher() {
/*     */     try {
/* 139 */       this.home = getHomeDirectory();
/* 140 */       initializeProperties();
/* 141 */       initializePaths();
/* 142 */       this.parent = createArchive();
/*     */     }
/* 144 */     catch (Exception ex) {
/* 145 */       throw new IllegalStateException(ex);
/*     */     } 
/*     */   }
/*     */   
/*     */   protected File getHomeDirectory() {
/*     */     try {
/* 151 */       return new File(getPropertyWithDefault("loader.home", "${user.dir}"));
/*     */     }
/* 153 */     catch (Exception ex) {
/* 154 */       throw new IllegalStateException(ex);
/*     */     } 
/*     */   }
/*     */   
/*     */   private void initializeProperties() throws Exception, IOException {
/* 159 */     List<String> configs = new ArrayList<>();
/* 160 */     if (getProperty("loader.config.location") != null) {
/* 161 */       configs.add(getProperty("loader.config.location"));
/*     */     } else {
/*     */       
/* 164 */       String[] names = getPropertyWithDefault("loader.config.name", "loader").split(",");
/* 165 */       for (String name : names) {
/* 166 */         configs.add("file:" + getHomeDirectory() + "/" + name + ".properties");
/* 167 */         configs.add("classpath:" + name + ".properties");
/* 168 */         configs.add("classpath:BOOT-INF/classes/" + name + ".properties");
/*     */       } 
/*     */     } 
/* 171 */     for (String config : configs) {
/* 172 */       try (InputStream resource = getResource(config)) {
/* 173 */         if (resource != null) {
/* 174 */           debug("Found: " + config);
/* 175 */           loadResource(resource);
/*     */           
/*     */           return;
/*     */         } 
/*     */         
/* 180 */         debug("Not found: " + config);
/*     */       } 
/*     */     } 
/*     */   }
/*     */ 
/*     */   
/*     */   private void loadResource(InputStream resource) throws IOException, Exception {
/* 187 */     this.properties.load(resource);
/* 188 */     for (Object key : Collections.list(this.properties.propertyNames())) {
/* 189 */       String text = this.properties.getProperty((String)key);
/* 190 */       String value = SystemPropertyUtils.resolvePlaceholders(this.properties, text);
/* 191 */       if (value != null) {
/* 192 */         this.properties.put(key, value);
/*     */       }
/*     */     } 
/* 195 */     if ("true".equals(getProperty("loader.system"))) {
/* 196 */       debug("Adding resolved properties to System properties");
/* 197 */       for (Object key : Collections.list(this.properties.propertyNames())) {
/* 198 */         String value = this.properties.getProperty((String)key);
/* 199 */         System.setProperty((String)key, value);
/*     */       } 
/*     */     } 
/*     */   }
/*     */   
/*     */   private InputStream getResource(String config) throws Exception {
/* 205 */     if (config.startsWith("classpath:")) {
/* 206 */       return getClasspathResource(config.substring("classpath:".length()));
/*     */     }
/* 208 */     config = handleUrl(config);
/* 209 */     if (isUrl(config)) {
/* 210 */       return getURLResource(config);
/*     */     }
/* 212 */     return getFileResource(config);
/*     */   }
/*     */   
/*     */   private String handleUrl(String path) throws UnsupportedEncodingException {
/* 216 */     if (path.startsWith("jar:file:") || path.startsWith("file:")) {
/* 217 */       path = URLDecoder.decode(path, "UTF-8");
/* 218 */       if (path.startsWith("file:")) {
/* 219 */         path = path.substring("file:".length());
/* 220 */         if (path.startsWith("//")) {
/* 221 */           path = path.substring(2);
/*     */         }
/*     */       } 
/*     */     } 
/* 225 */     return path;
/*     */   }
/*     */   
/*     */   private boolean isUrl(String config) {
/* 229 */     return config.contains("://");
/*     */   }
/*     */   
/*     */   private InputStream getClasspathResource(String config) {
/* 233 */     while (config.startsWith("/")) {
/* 234 */       config = config.substring(1);
/*     */     }
/* 236 */     config = "/" + config;
/* 237 */     debug("Trying classpath: " + config);
/* 238 */     return getClass().getResourceAsStream(config);
/*     */   }
/*     */   
/*     */   private InputStream getFileResource(String config) throws Exception {
/* 242 */     File file = new File(config);
/* 243 */     debug("Trying file: " + config);
/* 244 */     if (file.canRead()) {
/* 245 */       return new FileInputStream(file);
/*     */     }
/* 247 */     return null;
/*     */   }
/*     */   
/*     */   private InputStream getURLResource(String config) throws Exception {
/* 251 */     URL url = new URL(config);
/* 252 */     if (exists(url)) {
/* 253 */       URLConnection con = url.openConnection();
/*     */       try {
/* 255 */         return con.getInputStream();
/*     */       }
/* 257 */       catch (IOException ex) {
/*     */         
/* 259 */         if (con instanceof HttpURLConnection) {
/* 260 */           ((HttpURLConnection)con).disconnect();
/*     */         }
/* 262 */         throw ex;
/*     */       } 
/*     */     } 
/* 265 */     return null;
/*     */   }
/*     */ 
/*     */   
/*     */   private boolean exists(URL url) throws IOException {
/* 270 */     URLConnection connection = url.openConnection();
/*     */     try {
/* 272 */       connection.setUseCaches(connection
/* 273 */           .getClass().getSimpleName().startsWith("JNLP"));
/* 274 */       if (connection instanceof HttpURLConnection) {
/* 275 */         HttpURLConnection httpConnection = (HttpURLConnection)connection;
/* 276 */         httpConnection.setRequestMethod("HEAD");
/* 277 */         int responseCode = httpConnection.getResponseCode();
/* 278 */         if (responseCode == 200) {
/* 279 */           return true;
/*     */         }
/* 281 */         if (responseCode == 404) {
/* 282 */           return false;
/*     */         }
/*     */       } 
/* 285 */       return (connection.getContentLength() >= 0);
/*     */     } finally {
/*     */       
/* 288 */       if (connection instanceof HttpURLConnection) {
/* 289 */         ((HttpURLConnection)connection).disconnect();
/*     */       }
/*     */     } 
/*     */   }
/*     */   
/*     */   private void initializePaths() throws Exception {
/* 295 */     String path = getProperty("loader.path");
/* 296 */     if (path != null) {
/* 297 */       this.paths = parsePathsProperty(path);
/*     */     }
/* 299 */     debug("Nested archive paths: " + this.paths);
/*     */   }
/*     */   
/*     */   private List<String> parsePathsProperty(String commaSeparatedPaths) {
/* 303 */     List<String> paths = new ArrayList<>();
/* 304 */     for (String path : commaSeparatedPaths.split(",")) {
/* 305 */       path = cleanupPath(path);
/*     */       
/* 307 */       path = "".equals(path) ? "/" : path;
/* 308 */       paths.add(path);
/*     */     } 
/* 310 */     if (paths.isEmpty()) {
/* 311 */       paths.add("lib");
/*     */     }
/* 313 */     return paths;
/*     */   }
/*     */   
/*     */   protected String[] getArgs(String... args) throws Exception {
/* 317 */     String loaderArgs = getProperty("loader.args");
/* 318 */     if (loaderArgs != null) {
/* 319 */       String[] defaultArgs = loaderArgs.split("\\s+");
/* 320 */       String[] additionalArgs = args;
/* 321 */       args = new String[defaultArgs.length + additionalArgs.length];
/* 322 */       System.arraycopy(defaultArgs, 0, args, 0, defaultArgs.length);
/* 323 */       System.arraycopy(additionalArgs, 0, args, defaultArgs.length, additionalArgs.length);
/*     */     } 
/*     */     
/* 326 */     return args;
/*     */   }
/*     */ 
/*     */   
/*     */   protected String getMainClass() throws Exception {
/* 331 */     String mainClass = getProperty("loader.main", "Start-Class");
/* 332 */     if (mainClass == null) {
/* 333 */       throw new IllegalStateException("No 'loader.main' or 'Start-Class' specified");
/*     */     }
/*     */     
/* 336 */     return mainClass;
/*     */   }
/*     */ 
/*     */   
/*     */   protected ClassLoader createClassLoader(List<Archive> archives) throws Exception {
/* 341 */     Set<URL> urls = new LinkedHashSet<>(archives.size());
/* 342 */     for (Archive archive : archives) {
/* 343 */       urls.add(archive.getUrl());
/*     */     }
/*     */     
/* 346 */     ClassLoader loader = new LaunchedURLClassLoader(urls.<URL>toArray(new URL[0]), getClass().getClassLoader());
/* 347 */     debug("Classpath: " + urls);
/* 348 */     String customLoaderClassName = getProperty("loader.classLoader");
/* 349 */     if (customLoaderClassName != null) {
/* 350 */       loader = wrapWithCustomClassLoader(loader, customLoaderClassName);
/* 351 */       debug("Using custom class loader: " + customLoaderClassName);
/*     */     } 
/* 353 */     return loader;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   private ClassLoader wrapWithCustomClassLoader(ClassLoader parent, String loaderClassName) throws Exception {
/* 360 */     Class<ClassLoader> loaderClass = (Class)Class.forName(loaderClassName, true, parent);
/*     */     
/*     */     try {
/* 363 */       return loaderClass.getConstructor(new Class[] { ClassLoader.class }).newInstance(new Object[] { parent });
/*     */     }
/* 365 */     catch (NoSuchMethodException noSuchMethodException) {
/*     */ 
/*     */       
/*     */       try {
/* 369 */         return loaderClass.getConstructor(new Class[] { URL[].class, ClassLoader.class
/* 370 */             }).newInstance(new Object[] { new URL[0], parent });
/*     */       }
/* 372 */       catch (NoSuchMethodException noSuchMethodException1) {
/*     */ 
/*     */         
/* 375 */         return loaderClass.newInstance();
/*     */       } 
/*     */     } 
/*     */   } private String getProperty(String propertyKey) throws Exception {
/* 379 */     return getProperty(propertyKey, (String)null, (String)null);
/*     */   }
/*     */   
/*     */   private String getProperty(String propertyKey, String manifestKey) throws Exception {
/* 383 */     return getProperty(propertyKey, manifestKey, (String)null);
/*     */   }
/*     */ 
/*     */   
/*     */   private String getPropertyWithDefault(String propertyKey, String defaultValue) throws Exception {
/* 388 */     return getProperty(propertyKey, (String)null, defaultValue);
/*     */   }
/*     */ 
/*     */   
/*     */   private String getProperty(String propertyKey, String manifestKey, String defaultValue) throws Exception {
/* 393 */     if (manifestKey == null) {
/* 394 */       manifestKey = propertyKey.replace('.', '-');
/* 395 */       manifestKey = toCamelCase(manifestKey);
/*     */     } 
/* 397 */     String property = SystemPropertyUtils.getProperty(propertyKey);
/* 398 */     if (property != null) {
/* 399 */       String value = SystemPropertyUtils.resolvePlaceholders(this.properties, property);
/*     */       
/* 401 */       debug("Property '" + propertyKey + "' from environment: " + value);
/* 402 */       return value;
/*     */     } 
/* 404 */     if (this.properties.containsKey(propertyKey)) {
/* 405 */       String value = SystemPropertyUtils.resolvePlaceholders(this.properties, this.properties
/* 406 */           .getProperty(propertyKey));
/* 407 */       debug("Property '" + propertyKey + "' from properties: " + value);
/* 408 */       return value;
/*     */     } 
/*     */     try {
/* 411 */       if (this.home != null) {
/*     */         
/* 413 */         Manifest manifest1 = (new ExplodedArchive(this.home, false)).getManifest();
/* 414 */         if (manifest1 != null) {
/* 415 */           String value = manifest1.getMainAttributes().getValue(manifestKey);
/* 416 */           if (value != null) {
/* 417 */             debug("Property '" + manifestKey + "' from home directory manifest: " + value);
/*     */             
/* 419 */             return SystemPropertyUtils.resolvePlaceholders(this.properties, value);
/*     */           }
/*     */         
/*     */         }
/*     */       
/*     */       } 
/* 425 */     } catch (IllegalStateException illegalStateException) {}
/*     */ 
/*     */ 
/*     */     
/* 429 */     Manifest manifest = createArchive().getManifest();
/* 430 */     if (manifest != null) {
/* 431 */       String value = manifest.getMainAttributes().getValue(manifestKey);
/* 432 */       if (value != null) {
/* 433 */         debug("Property '" + manifestKey + "' from archive manifest: " + value);
/* 434 */         return SystemPropertyUtils.resolvePlaceholders(this.properties, value);
/*     */       } 
/*     */     } 
/* 437 */     return (defaultValue != null) ? 
/* 438 */       SystemPropertyUtils.resolvePlaceholders(this.properties, defaultValue) : defaultValue;
/*     */   }
/*     */ 
/*     */ 
/*     */   
/*     */   protected List<Archive> getClassPathArchives() throws Exception {
/* 444 */     List<Archive> lib = new ArrayList<>();
/* 445 */     for (String path : this.paths) {
/* 446 */       for (Archive archive : getClassPathArchives(path)) {
/* 447 */         if (archive instanceof ExplodedArchive) {
/*     */           
/* 449 */           List<Archive> nested = new ArrayList<>(archive.getNestedArchives(new ArchiveEntryFilter()));
/* 450 */           nested.add(0, archive);
/* 451 */           lib.addAll(nested);
/*     */           continue;
/*     */         } 
/* 454 */         lib.add(archive);
/*     */       } 
/*     */     } 
/*     */     
/* 458 */     addNestedEntries(lib);
/* 459 */     return lib;
/*     */   }
/*     */   
/*     */   private List<Archive> getClassPathArchives(String path) throws Exception {
/* 463 */     String root = cleanupPath(handleUrl(path));
/* 464 */     List<Archive> lib = new ArrayList<>();
/* 465 */     File file = new File(root);
/* 466 */     if (!"/".equals(root)) {
/* 467 */       if (!isAbsolutePath(root)) {
/* 468 */         file = new File(this.home, root);
/*     */       }
/* 470 */       if (file.isDirectory()) {
/* 471 */         debug("Adding classpath entries from " + file);
/* 472 */         ExplodedArchive explodedArchive = new ExplodedArchive(file, false);
/* 473 */         lib.add(explodedArchive);
/*     */       } 
/*     */     } 
/* 476 */     Archive archive = getArchive(file);
/* 477 */     if (archive != null) {
/* 478 */       debug("Adding classpath entries from archive " + archive.getUrl() + root);
/* 479 */       lib.add(archive);
/*     */     } 
/* 481 */     List<Archive> nestedArchives = getNestedArchives(root);
/* 482 */     if (nestedArchives != null) {
/* 483 */       debug("Adding classpath entries from nested " + root);
/* 484 */       lib.addAll(nestedArchives);
/*     */     } 
/* 486 */     return lib;
/*     */   }
/*     */ 
/*     */   
/*     */   private boolean isAbsolutePath(String root) {
/* 491 */     return (root.contains(":") || root.startsWith("/"));
/*     */   }
/*     */   
/*     */   private Archive getArchive(File file) throws IOException {
/* 495 */     if (isNestedArchivePath(file)) {
/* 496 */       return null;
/*     */     }
/* 498 */     String name = file.getName().toLowerCase(Locale.ENGLISH);
/* 499 */     if (name.endsWith(".jar") || name.endsWith(".zip")) {
/* 500 */       return (Archive)new JarFileArchive(file);
/*     */     }
/* 502 */     return null;
/*     */   }
/*     */   
/*     */   private boolean isNestedArchivePath(File file) {
/* 506 */     return file.getPath().contains(NESTED_ARCHIVE_SEPARATOR);
/*     */   }
/*     */   private List<Archive> getNestedArchives(String path) throws Exception {
/*     */     JarFileArchive jarFileArchive;
/* 510 */     Archive parent = this.parent;
/* 511 */     String root = path;
/* 512 */     if ((!root.equals("/") && root.startsWith("/")) || parent
/* 513 */       .getUrl().equals(this.home.toURI().toURL()))
/*     */     {
/* 515 */       return null;
/*     */     }
/* 517 */     int index = root.indexOf('!');
/* 518 */     if (index != -1) {
/* 519 */       File file = new File(this.home, root.substring(0, index));
/* 520 */       if (root.startsWith("jar:file:")) {
/* 521 */         file = new File(root.substring("jar:file:".length(), index));
/*     */       }
/* 523 */       jarFileArchive = new JarFileArchive(file);
/* 524 */       root = root.substring(index + 1);
/* 525 */       while (root.startsWith("/")) {
/* 526 */         root = root.substring(1);
/*     */       }
/*     */     } 
/* 529 */     if (root.endsWith(".jar")) {
/* 530 */       File file = new File(this.home, root);
/* 531 */       if (file.exists()) {
/* 532 */         jarFileArchive = new JarFileArchive(file);
/* 533 */         root = "";
/*     */       } 
/*     */     } 
/* 536 */     if (root.equals("/") || root.equals("./") || root.equals("."))
/*     */     {
/* 538 */       root = "";
/*     */     }
/* 540 */     Archive.EntryFilter filter = new PrefixMatchingArchiveFilter(root);
/* 541 */     List<Archive> archives = new ArrayList<>(jarFileArchive.getNestedArchives(filter));
/* 542 */     if (("".equals(root) || ".".equals(root)) && !path.endsWith(".jar") && jarFileArchive != this.parent)
/*     */     {
/*     */ 
/*     */       
/* 546 */       archives.add(jarFileArchive);
/*     */     }
/* 548 */     return archives;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   private void addNestedEntries(List<Archive> lib) {
/*     */     try {
/* 556 */       lib.addAll(this.parent.getNestedArchives(entry -> entry.isDirectory() ? entry.getName().equals("BOOT-INF/classes/") : entry.getName().startsWith("BOOT-INF/lib/")));
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */     
/*     */     }
/* 563 */     catch (IOException iOException) {}
/*     */   }
/*     */ 
/*     */ 
/*     */   
/*     */   private String cleanupPath(String path) {
/* 569 */     path = path.trim();
/*     */     
/* 571 */     if (path.startsWith("./")) {
/* 572 */       path = path.substring(2);
/*     */     }
/* 574 */     String lowerCasePath = path.toLowerCase(Locale.ENGLISH);
/* 575 */     if (lowerCasePath.endsWith(".jar") || lowerCasePath.endsWith(".zip")) {
/* 576 */       return path;
/*     */     }
/* 578 */     if (path.endsWith("/*")) {
/* 579 */       path = path.substring(0, path.length() - 1);
/*     */ 
/*     */     
/*     */     }
/* 583 */     else if (!path.endsWith("/") && !path.equals(".")) {
/* 584 */       path = path + "/";
/*     */     } 
/*     */     
/* 587 */     return path;
/*     */   }
/*     */   
/*     */   public static void main(String[] args) throws Exception {
/* 591 */     PropertiesLauncher launcher = new PropertiesLauncher();
/* 592 */     args = launcher.getArgs(args);
/* 593 */     launcher.launch(args);
/*     */   }
/*     */   
/*     */   public static String toCamelCase(CharSequence string) {
/* 597 */     if (string == null) {
/* 598 */       return null;
/*     */     }
/* 600 */     StringBuilder builder = new StringBuilder();
/* 601 */     Matcher matcher = WORD_SEPARATOR.matcher(string);
/* 602 */     int pos = 0;
/* 603 */     while (matcher.find()) {
/* 604 */       builder.append(capitalize(string.subSequence(pos, matcher.end()).toString()));
/* 605 */       pos = matcher.end();
/*     */     } 
/* 607 */     builder.append(capitalize(string.subSequence(pos, string.length()).toString()));
/* 608 */     return builder.toString();
/*     */   }
/*     */   
/*     */   private static String capitalize(String str) {
/* 612 */     return Character.toUpperCase(str.charAt(0)) + str.substring(1);
/*     */   }
/*     */   
/*     */   private void debug(String message) {
/* 616 */     if (Boolean.getBoolean("loader.debug")) {
/* 617 */       System.out.println(message);
/*     */     }
/*     */   }
/*     */ 
/*     */ 
/*     */   
/*     */   private static final class PrefixMatchingArchiveFilter
/*     */     implements Archive.EntryFilter
/*     */   {
/*     */     private final String prefix;
/*     */ 
/*     */     
/* 629 */     private final PropertiesLauncher.ArchiveEntryFilter filter = new PropertiesLauncher.ArchiveEntryFilter();
/*     */     
/*     */     private PrefixMatchingArchiveFilter(String prefix) {
/* 632 */       this.prefix = prefix;
/*     */     }
/*     */ 
/*     */     
/*     */     public boolean matches(Archive.Entry entry) {
/* 637 */       if (entry.isDirectory()) {
/* 638 */         return entry.getName().equals(this.prefix);
/*     */       }
/* 640 */       return (entry.getName().startsWith(this.prefix) && this.filter.matches(entry));
/*     */     }
/*     */   }
/*     */ 
/*     */   
/*     */   private static final class ArchiveEntryFilter
/*     */     implements Archive.EntryFilter
/*     */   {
/*     */     private static final String DOT_JAR = ".jar";
/*     */     
/*     */     private static final String DOT_ZIP = ".zip";
/*     */ 
/*     */     
/*     */     private ArchiveEntryFilter() {}
/*     */ 
/*     */     
/*     */     public boolean matches(Archive.Entry entry) {
/* 657 */       return (entry.getName().endsWith(".jar") || entry.getName().endsWith(".zip"));
/*     */     }
/*     */   }
/*     */ }


/* Location:              C:\Users\zhr_0\Desktop\challenge-0.0.1-SNAPSHOT.jar!\org\springframework\boot\loader\PropertiesLauncher.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */