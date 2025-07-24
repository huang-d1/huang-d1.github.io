/*     */ package org.springframework.boot.loader.jar;
/*     */ 
/*     */ import java.io.IOException;
/*     */ import java.io.InputStream;
/*     */ import java.util.Arrays;
/*     */ import java.util.Collections;
/*     */ import java.util.Iterator;
/*     */ import java.util.LinkedHashMap;
/*     */ import java.util.Map;
/*     */ import java.util.NoSuchElementException;
/*     */ import org.springframework.boot.loader.data.RandomAccessData;
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
/*     */ class JarFileEntries
/*     */   implements CentralDirectoryVisitor, Iterable<JarEntry>
/*     */ {
/*     */   private static final long LOCAL_FILE_HEADER_SIZE = 30L;
/*     */   private static final char SLASH = '/';
/*     */   private static final char NO_SUFFIX = '\000';
/*     */   protected static final int ENTRY_CACHE_SIZE = 25;
/*     */   private final JarFile jarFile;
/*     */   private final JarEntryFilter filter;
/*     */   private RandomAccessData centralDirectoryData;
/*     */   private int size;
/*     */   private int[] hashCodes;
/*     */   private int[] centralDirectoryOffsets;
/*     */   private int[] positions;
/*     */   
/*  70 */   private final Map<Integer, FileHeader> entriesCache = Collections.synchronizedMap(new LinkedHashMap<Integer, FileHeader>(16, 0.75F, true)
/*     */       {
/*     */         
/*     */         protected boolean removeEldestEntry(Map.Entry<Integer, FileHeader> eldest)
/*     */         {
/*  75 */           if (JarFileEntries.this.jarFile.isSigned()) {
/*  76 */             return false;
/*     */           }
/*  78 */           return (size() >= 25);
/*     */         }
/*     */       });
/*     */ 
/*     */   
/*     */   JarFileEntries(JarFile jarFile, JarEntryFilter filter) {
/*  84 */     this.jarFile = jarFile;
/*  85 */     this.filter = filter;
/*     */   }
/*     */ 
/*     */ 
/*     */   
/*     */   public void visitStart(CentralDirectoryEndRecord endRecord, RandomAccessData centralDirectoryData) {
/*  91 */     int maxSize = endRecord.getNumberOfRecords();
/*  92 */     this.centralDirectoryData = centralDirectoryData;
/*  93 */     this.hashCodes = new int[maxSize];
/*  94 */     this.centralDirectoryOffsets = new int[maxSize];
/*  95 */     this.positions = new int[maxSize];
/*     */   }
/*     */ 
/*     */   
/*     */   public void visitFileHeader(CentralDirectoryFileHeader fileHeader, int dataOffset) {
/* 100 */     AsciiBytes name = applyFilter(fileHeader.getName());
/* 101 */     if (name != null) {
/* 102 */       add(name, dataOffset);
/*     */     }
/*     */   }
/*     */   
/*     */   private void add(AsciiBytes name, int dataOffset) {
/* 107 */     this.hashCodes[this.size] = name.hashCode();
/* 108 */     this.centralDirectoryOffsets[this.size] = dataOffset;
/* 109 */     this.positions[this.size] = this.size;
/* 110 */     this.size++;
/*     */   }
/*     */ 
/*     */   
/*     */   public void visitEnd() {
/* 115 */     sort(0, this.size - 1);
/* 116 */     int[] positions = this.positions;
/* 117 */     this.positions = new int[positions.length];
/* 118 */     for (int i = 0; i < this.size; i++) {
/* 119 */       this.positions[positions[i]] = i;
/*     */     }
/*     */   }
/*     */   
/*     */   int getSize() {
/* 124 */     return this.size;
/*     */   }
/*     */ 
/*     */   
/*     */   private void sort(int left, int right) {
/* 129 */     if (left < right) {
/* 130 */       int pivot = this.hashCodes[left + (right - left) / 2];
/* 131 */       int i = left;
/* 132 */       int j = right;
/* 133 */       while (i <= j) {
/* 134 */         while (this.hashCodes[i] < pivot) {
/* 135 */           i++;
/*     */         }
/* 137 */         while (this.hashCodes[j] > pivot) {
/* 138 */           j--;
/*     */         }
/* 140 */         if (i <= j) {
/* 141 */           swap(i, j);
/* 142 */           i++;
/* 143 */           j--;
/*     */         } 
/*     */       } 
/* 146 */       if (left < j) {
/* 147 */         sort(left, j);
/*     */       }
/* 149 */       if (right > i) {
/* 150 */         sort(i, right);
/*     */       }
/*     */     } 
/*     */   }
/*     */   
/*     */   private void swap(int i, int j) {
/* 156 */     swap(this.hashCodes, i, j);
/* 157 */     swap(this.centralDirectoryOffsets, i, j);
/* 158 */     swap(this.positions, i, j);
/*     */   }
/*     */   
/*     */   private void swap(int[] array, int i, int j) {
/* 162 */     int temp = array[i];
/* 163 */     array[i] = array[j];
/* 164 */     array[j] = temp;
/*     */   }
/*     */ 
/*     */   
/*     */   public Iterator<JarEntry> iterator() {
/* 169 */     return new EntryIterator();
/*     */   }
/*     */   
/*     */   public boolean containsEntry(CharSequence name) {
/* 173 */     return (getEntry(name, FileHeader.class, true) != null);
/*     */   }
/*     */   
/*     */   public JarEntry getEntry(CharSequence name) {
/* 177 */     return getEntry(name, JarEntry.class, true);
/*     */   }
/*     */   
/*     */   public InputStream getInputStream(String name) throws IOException {
/* 181 */     FileHeader entry = getEntry(name, FileHeader.class, false);
/* 182 */     return getInputStream(entry);
/*     */   }
/*     */   
/*     */   public InputStream getInputStream(FileHeader entry) throws IOException {
/* 186 */     if (entry == null) {
/* 187 */       return null;
/*     */     }
/* 189 */     InputStream inputStream = getEntryData(entry).getInputStream();
/* 190 */     if (entry.getMethod() == 8) {
/* 191 */       inputStream = new ZipInflaterInputStream(inputStream, (int)entry.getSize());
/*     */     }
/* 193 */     return inputStream;
/*     */   }
/*     */   
/*     */   public RandomAccessData getEntryData(String name) throws IOException {
/* 197 */     FileHeader entry = getEntry(name, FileHeader.class, false);
/* 198 */     if (entry == null) {
/* 199 */       return null;
/*     */     }
/* 201 */     return getEntryData(entry);
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   private RandomAccessData getEntryData(FileHeader entry) throws IOException {
/* 208 */     RandomAccessData data = this.jarFile.getData();
/* 209 */     byte[] localHeader = data.read(entry.getLocalHeaderOffset(), 30L);
/*     */     
/* 211 */     long nameLength = Bytes.littleEndianValue(localHeader, 26, 2);
/* 212 */     long extraLength = Bytes.littleEndianValue(localHeader, 28, 2);
/* 213 */     return data.getSubsection(entry.getLocalHeaderOffset() + 30L + nameLength + extraLength, entry
/* 214 */         .getCompressedSize());
/*     */   }
/*     */ 
/*     */   
/*     */   private <T extends FileHeader> T getEntry(CharSequence name, Class<T> type, boolean cacheEntry) {
/* 219 */     int hashCode = AsciiBytes.hashCode(name);
/* 220 */     T entry = getEntry(hashCode, name, false, type, cacheEntry);
/* 221 */     if (entry == null) {
/* 222 */       hashCode = AsciiBytes.hashCode(hashCode, '/');
/* 223 */       entry = getEntry(hashCode, name, '/', type, cacheEntry);
/*     */     } 
/* 225 */     return entry;
/*     */   }
/*     */ 
/*     */   
/*     */   private <T extends FileHeader> T getEntry(int hashCode, CharSequence name, char suffix, Class<T> type, boolean cacheEntry) {
/* 230 */     int index = getFirstIndex(hashCode);
/* 231 */     while (index >= 0 && index < this.size && this.hashCodes[index] == hashCode) {
/* 232 */       T entry = getEntry(index, type, cacheEntry);
/* 233 */       if (entry.hasName(name, suffix)) {
/* 234 */         return entry;
/*     */       }
/* 236 */       index++;
/*     */     } 
/* 238 */     return null;
/*     */   }
/*     */ 
/*     */ 
/*     */   
/*     */   private <T extends FileHeader> T getEntry(int index, Class<T> type, boolean cacheEntry) {
/*     */     try {
/* 245 */       FileHeader cached = this.entriesCache.get(Integer.valueOf(index));
/*     */       
/* 247 */       FileHeader entry = (cached != null) ? cached : CentralDirectoryFileHeader.fromRandomAccessData(this.centralDirectoryData, this.centralDirectoryOffsets[index], this.filter);
/*     */ 
/*     */       
/* 250 */       if (CentralDirectoryFileHeader.class.equals(entry.getClass()) && type
/* 251 */         .equals(JarEntry.class)) {
/* 252 */         entry = new JarEntry(this.jarFile, (CentralDirectoryFileHeader)entry);
/*     */       }
/* 254 */       if (cacheEntry && cached != entry) {
/* 255 */         this.entriesCache.put(Integer.valueOf(index), entry);
/*     */       }
/* 257 */       return (T)entry;
/*     */     }
/* 259 */     catch (IOException ex) {
/* 260 */       throw new IllegalStateException(ex);
/*     */     } 
/*     */   }
/*     */   
/*     */   private int getFirstIndex(int hashCode) {
/* 265 */     int index = Arrays.binarySearch(this.hashCodes, 0, this.size, hashCode);
/* 266 */     if (index < 0) {
/* 267 */       return -1;
/*     */     }
/* 269 */     while (index > 0 && this.hashCodes[index - 1] == hashCode) {
/* 270 */       index--;
/*     */     }
/* 272 */     return index;
/*     */   }
/*     */   
/*     */   public void clearCache() {
/* 276 */     this.entriesCache.clear();
/*     */   }
/*     */   
/*     */   private AsciiBytes applyFilter(AsciiBytes name) {
/* 280 */     return (this.filter != null) ? this.filter.apply(name) : name;
/*     */   }
/*     */ 
/*     */ 
/*     */   
/*     */   private class EntryIterator
/*     */     implements Iterator<JarEntry>
/*     */   {
/* 288 */     private int index = 0;
/*     */ 
/*     */     
/*     */     public boolean hasNext() {
/* 292 */       return (this.index < JarFileEntries.this.size);
/*     */     }
/*     */ 
/*     */     
/*     */     public JarEntry next() {
/* 297 */       if (!hasNext()) {
/* 298 */         throw new NoSuchElementException();
/*     */       }
/* 300 */       int entryIndex = JarFileEntries.this.positions[this.index];
/* 301 */       this.index++;
/* 302 */       return (JarEntry)JarFileEntries.this.getEntry(entryIndex, (Class)JarEntry.class, false);
/*     */     }
/*     */     
/*     */     private EntryIterator() {}
/*     */   }
/*     */ }


/* Location:              C:\Users\zhr_0\Desktop\challenge-0.0.1-SNAPSHOT.jar!\org\springframework\boot\loader\jar\JarFileEntries.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */