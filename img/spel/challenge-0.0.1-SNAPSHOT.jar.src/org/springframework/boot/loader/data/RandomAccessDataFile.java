/*     */ package org.springframework.boot.loader.data;
/*     */ 
/*     */ import java.io.EOFException;
/*     */ import java.io.File;
/*     */ import java.io.FileNotFoundException;
/*     */ import java.io.IOException;
/*     */ import java.io.InputStream;
/*     */ import java.io.RandomAccessFile;
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
/*     */ public class RandomAccessDataFile
/*     */   implements RandomAccessData
/*     */ {
/*     */   private final FileAccess fileAccess;
/*     */   private final long offset;
/*     */   private final long length;
/*     */   
/*     */   public RandomAccessDataFile(File file) {
/*  46 */     if (file == null) {
/*  47 */       throw new IllegalArgumentException("File must not be null");
/*     */     }
/*  49 */     this.fileAccess = new FileAccess(file);
/*  50 */     this.offset = 0L;
/*  51 */     this.length = file.length();
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   private RandomAccessDataFile(FileAccess fileAccess, long offset, long length) {
/*  61 */     this.fileAccess = fileAccess;
/*  62 */     this.offset = offset;
/*  63 */     this.length = length;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public File getFile() {
/*  71 */     return this.fileAccess.file;
/*     */   }
/*     */ 
/*     */   
/*     */   public InputStream getInputStream() throws IOException {
/*  76 */     return new DataInputStream();
/*     */   }
/*     */ 
/*     */   
/*     */   public RandomAccessData getSubsection(long offset, long length) {
/*  81 */     if (offset < 0L || length < 0L || offset + length > this.length) {
/*  82 */       throw new IndexOutOfBoundsException();
/*     */     }
/*  84 */     return new RandomAccessDataFile(this.fileAccess, this.offset + offset, length);
/*     */   }
/*     */ 
/*     */   
/*     */   public byte[] read() throws IOException {
/*  89 */     return read(0L, this.length);
/*     */   }
/*     */ 
/*     */   
/*     */   public byte[] read(long offset, long length) throws IOException {
/*  94 */     if (offset > this.length) {
/*  95 */       throw new IndexOutOfBoundsException();
/*     */     }
/*  97 */     if (offset + length > this.length) {
/*  98 */       throw new EOFException();
/*     */     }
/* 100 */     byte[] bytes = new byte[(int)length];
/* 101 */     read(bytes, offset, 0, bytes.length);
/* 102 */     return bytes;
/*     */   }
/*     */   
/*     */   private int readByte(long position) throws IOException {
/* 106 */     if (position >= this.length) {
/* 107 */       return -1;
/*     */     }
/* 109 */     return this.fileAccess.readByte(this.offset + position);
/*     */   }
/*     */ 
/*     */   
/*     */   private int read(byte[] bytes, long position, int offset, int length) throws IOException {
/* 114 */     if (position > this.length) {
/* 115 */       return -1;
/*     */     }
/* 117 */     return this.fileAccess.read(bytes, this.offset + position, offset, length);
/*     */   }
/*     */ 
/*     */   
/*     */   public long getSize() {
/* 122 */     return this.length;
/*     */   }
/*     */   
/*     */   public void close() throws IOException {
/* 126 */     this.fileAccess.close();
/*     */   }
/*     */ 
/*     */   
/*     */   private class DataInputStream
/*     */     extends InputStream
/*     */   {
/*     */     private int position;
/*     */     
/*     */     private DataInputStream() {}
/*     */     
/*     */     public int read() throws IOException {
/* 138 */       int read = RandomAccessDataFile.this.readByte(this.position);
/* 139 */       if (read > -1) {
/* 140 */         moveOn(1);
/*     */       }
/* 142 */       return read;
/*     */     }
/*     */ 
/*     */     
/*     */     public int read(byte[] b) throws IOException {
/* 147 */       return read(b, 0, (b != null) ? b.length : 0);
/*     */     }
/*     */ 
/*     */     
/*     */     public int read(byte[] b, int off, int len) throws IOException {
/* 152 */       if (b == null) {
/* 153 */         throw new NullPointerException("Bytes must not be null");
/*     */       }
/* 155 */       return doRead(b, off, len);
/*     */     }
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
/*     */     public int doRead(byte[] b, int off, int len) throws IOException {
/* 168 */       if (len == 0) {
/* 169 */         return 0;
/*     */       }
/* 171 */       int cappedLen = cap(len);
/* 172 */       if (cappedLen <= 0) {
/* 173 */         return -1;
/*     */       }
/* 175 */       return (int)moveOn(RandomAccessDataFile.this
/* 176 */           .read(b, this.position, off, cappedLen));
/*     */     }
/*     */ 
/*     */     
/*     */     public long skip(long n) throws IOException {
/* 181 */       return (n <= 0L) ? 0L : moveOn(cap(n));
/*     */     }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */     
/*     */     private int cap(long n) {
/* 191 */       return (int)Math.min(RandomAccessDataFile.this.length - this.position, n);
/*     */     }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */     
/*     */     private long moveOn(int amount) {
/* 200 */       this.position += amount;
/* 201 */       return amount;
/*     */     }
/*     */   }
/*     */ 
/*     */   
/*     */   private static final class FileAccess
/*     */   {
/* 208 */     private final Object monitor = new Object();
/*     */     
/*     */     private final File file;
/*     */     
/*     */     private RandomAccessFile randomAccessFile;
/*     */     
/*     */     private FileAccess(File file) {
/* 215 */       this.file = file;
/* 216 */       openIfNecessary();
/*     */     }
/*     */ 
/*     */     
/*     */     private int read(byte[] bytes, long position, int offset, int length) throws IOException {
/* 221 */       synchronized (this.monitor) {
/* 222 */         openIfNecessary();
/* 223 */         this.randomAccessFile.seek(position);
/* 224 */         return this.randomAccessFile.read(bytes, offset, length);
/*     */       } 
/*     */     }
/*     */     
/*     */     private void openIfNecessary() {
/* 229 */       if (this.randomAccessFile == null) {
/*     */         try {
/* 231 */           this.randomAccessFile = new RandomAccessFile(this.file, "r");
/*     */         }
/* 233 */         catch (FileNotFoundException ex) {
/* 234 */           throw new IllegalArgumentException(String.format("File %s must exist", new Object[] { this.file
/* 235 */                   .getAbsolutePath() }));
/*     */         } 
/*     */       }
/*     */     }
/*     */     
/*     */     private void close() throws IOException {
/* 241 */       synchronized (this.monitor) {
/* 242 */         if (this.randomAccessFile != null) {
/* 243 */           this.randomAccessFile.close();
/* 244 */           this.randomAccessFile = null;
/*     */         } 
/*     */       } 
/*     */     }
/*     */     
/*     */     private int readByte(long position) throws IOException {
/* 250 */       synchronized (this.monitor) {
/* 251 */         openIfNecessary();
/* 252 */         this.randomAccessFile.seek(position);
/* 253 */         return this.randomAccessFile.read();
/*     */       } 
/*     */     }
/*     */   }
/*     */ }


/* Location:              C:\Users\zhr_0\Desktop\challenge-0.0.1-SNAPSHOT.jar!\org\springframework\boot\loader\data\RandomAccessDataFile.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */