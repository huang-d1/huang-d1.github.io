/*     */ package org.springframework.boot.loader.archive;
/*     */ 
/*     */ import java.io.File;
/*     */ import java.io.FileInputStream;
/*     */ import java.io.IOException;
/*     */ import java.net.MalformedURLException;
/*     */ import java.net.URL;
/*     */ import java.util.ArrayList;
/*     */ import java.util.Arrays;
/*     */ import java.util.Collections;
/*     */ import java.util.Comparator;
/*     */ import java.util.Deque;
/*     */ import java.util.HashSet;
/*     */ import java.util.Iterator;
/*     */ import java.util.LinkedList;
/*     */ import java.util.List;
/*     */ import java.util.NoSuchElementException;
/*     */ import java.util.Set;
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
/*     */ public class ExplodedArchive
/*     */   implements Archive
/*     */ {
/*  45 */   private static final Set<String> SKIPPED_NAMES = new HashSet<>(
/*  46 */       Arrays.asList(new String[] { ".", ".." }));
/*     */ 
/*     */   
/*     */   private final File root;
/*     */ 
/*     */   
/*     */   private final boolean recursive;
/*     */ 
/*     */   
/*     */   private File manifestFile;
/*     */   
/*     */   private Manifest manifest;
/*     */ 
/*     */   
/*     */   public ExplodedArchive(File root) {
/*  61 */     this(root, true);
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
/*     */   public ExplodedArchive(File root, boolean recursive) {
/*  73 */     if (!root.exists() || !root.isDirectory()) {
/*  74 */       throw new IllegalArgumentException("Invalid source folder " + root);
/*     */     }
/*  76 */     this.root = root;
/*  77 */     this.recursive = recursive;
/*  78 */     this.manifestFile = getManifestFile(root);
/*     */   }
/*     */   
/*     */   private File getManifestFile(File root) {
/*  82 */     File metaInf = new File(root, "META-INF");
/*  83 */     return new File(metaInf, "MANIFEST.MF");
/*     */   }
/*     */ 
/*     */   
/*     */   public URL getUrl() throws MalformedURLException {
/*  88 */     return this.root.toURI().toURL();
/*     */   }
/*     */ 
/*     */   
/*     */   public Manifest getManifest() throws IOException {
/*  93 */     if (this.manifest == null && this.manifestFile.exists()) {
/*  94 */       try (FileInputStream inputStream = new FileInputStream(this.manifestFile)) {
/*  95 */         this.manifest = new Manifest(inputStream);
/*     */       } 
/*     */     }
/*  98 */     return this.manifest;
/*     */   }
/*     */ 
/*     */   
/*     */   public List<Archive> getNestedArchives(Archive.EntryFilter filter) throws IOException {
/* 103 */     List<Archive> nestedArchives = new ArrayList<>();
/* 104 */     for (Archive.Entry entry : this) {
/* 105 */       if (filter.matches(entry)) {
/* 106 */         nestedArchives.add(getNestedArchive(entry));
/*     */       }
/*     */     } 
/* 109 */     return Collections.unmodifiableList(nestedArchives);
/*     */   }
/*     */ 
/*     */   
/*     */   public Iterator<Archive.Entry> iterator() {
/* 114 */     return new FileEntryIterator(this.root, this.recursive);
/*     */   }
/*     */   
/*     */   protected Archive getNestedArchive(Archive.Entry entry) throws IOException {
/* 118 */     File file = ((FileEntry)entry).getFile();
/* 119 */     return file.isDirectory() ? new ExplodedArchive(file) : new JarFileArchive(file);
/*     */   }
/*     */ 
/*     */ 
/*     */   
/*     */   public String toString() {
/*     */     try {
/* 126 */       return getUrl().toString();
/*     */     }
/* 128 */     catch (Exception ex) {
/* 129 */       return "exploded archive";
/*     */     } 
/*     */   }
/*     */ 
/*     */ 
/*     */   
/*     */   private static class FileEntryIterator
/*     */     implements Iterator<Archive.Entry>
/*     */   {
/* 138 */     private final Comparator<File> entryComparator = new EntryComparator();
/*     */     
/*     */     private final File root;
/*     */     
/*     */     private final boolean recursive;
/*     */     
/* 144 */     private final Deque<Iterator<File>> stack = new LinkedList<>();
/*     */     
/*     */     private File current;
/*     */     
/*     */     FileEntryIterator(File root, boolean recursive) {
/* 149 */       this.root = root;
/* 150 */       this.recursive = recursive;
/* 151 */       this.stack.add(listFiles(root));
/* 152 */       this.current = poll();
/*     */     }
/*     */ 
/*     */     
/*     */     public boolean hasNext() {
/* 157 */       return (this.current != null);
/*     */     }
/*     */ 
/*     */     
/*     */     public Archive.Entry next() {
/* 162 */       if (this.current == null) {
/* 163 */         throw new NoSuchElementException();
/*     */       }
/* 165 */       File file = this.current;
/* 166 */       if (file.isDirectory() && (this.recursive || file
/* 167 */         .getParentFile().equals(this.root))) {
/* 168 */         this.stack.addFirst(listFiles(file));
/*     */       }
/* 170 */       this.current = poll();
/*     */       
/* 172 */       String name = file.toURI().getPath().substring(this.root.toURI().getPath().length());
/* 173 */       return new ExplodedArchive.FileEntry(name, file);
/*     */     }
/*     */     
/*     */     private Iterator<File> listFiles(File file) {
/* 177 */       File[] files = file.listFiles();
/* 178 */       if (files == null) {
/* 179 */         return Collections.<File>emptyList().iterator();
/*     */       }
/* 181 */       Arrays.sort(files, this.entryComparator);
/* 182 */       return Arrays.<File>asList(files).iterator();
/*     */     }
/*     */     
/*     */     private File poll() {
/* 186 */       while (!this.stack.isEmpty()) {
/* 187 */         while (((Iterator)this.stack.peek()).hasNext()) {
/* 188 */           File file = ((Iterator<File>)this.stack.peek()).next();
/* 189 */           if (!ExplodedArchive.SKIPPED_NAMES.contains(file.getName())) {
/* 190 */             return file;
/*     */           }
/*     */         } 
/* 193 */         this.stack.poll();
/*     */       } 
/* 195 */       return null;
/*     */     }
/*     */ 
/*     */     
/*     */     public void remove() {
/* 200 */       throw new UnsupportedOperationException("remove");
/*     */     }
/*     */ 
/*     */     
/*     */     private static class EntryComparator
/*     */       implements Comparator<File>
/*     */     {
/*     */       private EntryComparator() {}
/*     */       
/*     */       public int compare(File o1, File o2) {
/* 210 */         return o1.getAbsolutePath().compareTo(o2.getAbsolutePath());
/*     */       }
/*     */     }
/*     */   }
/*     */ 
/*     */ 
/*     */   
/*     */   private static class FileEntry
/*     */     implements Archive.Entry
/*     */   {
/*     */     private final String name;
/*     */ 
/*     */     
/*     */     private final File file;
/*     */ 
/*     */     
/*     */     FileEntry(String name, File file) {
/* 227 */       this.name = name;
/* 228 */       this.file = file;
/*     */     }
/*     */     
/*     */     public File getFile() {
/* 232 */       return this.file;
/*     */     }
/*     */ 
/*     */     
/*     */     public boolean isDirectory() {
/* 237 */       return this.file.isDirectory();
/*     */     }
/*     */ 
/*     */     
/*     */     public String getName() {
/* 242 */       return this.name;
/*     */     }
/*     */   }
/*     */ }


/* Location:              C:\Users\zhr_0\Desktop\challenge-0.0.1-SNAPSHOT.jar!\org\springframework\boot\loader\archive\ExplodedArchive.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */