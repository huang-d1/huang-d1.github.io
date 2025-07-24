/*     */ package org.springframework.boot.loader.jar;
/*     */ 
/*     */ import java.io.IOException;
/*     */ import java.time.LocalDateTime;
/*     */ import java.time.ZoneId;
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
/*     */ final class CentralDirectoryFileHeader
/*     */   implements FileHeader
/*     */ {
/*  35 */   private static final AsciiBytes SLASH = new AsciiBytes("/");
/*     */   
/*  37 */   private static final byte[] NO_EXTRA = new byte[0];
/*     */   
/*  39 */   private static final AsciiBytes NO_COMMENT = new AsciiBytes("");
/*     */   
/*     */   private byte[] header;
/*     */   
/*     */   private int headerOffset;
/*     */   
/*     */   private AsciiBytes name;
/*     */   
/*     */   private byte[] extra;
/*     */   
/*     */   private AsciiBytes comment;
/*     */   
/*     */   private long localHeaderOffset;
/*     */ 
/*     */   
/*     */   CentralDirectoryFileHeader() {}
/*     */ 
/*     */   
/*     */   CentralDirectoryFileHeader(byte[] header, int headerOffset, AsciiBytes name, byte[] extra, AsciiBytes comment, long localHeaderOffset) {
/*  58 */     this.header = header;
/*  59 */     this.headerOffset = headerOffset;
/*  60 */     this.name = name;
/*  61 */     this.extra = extra;
/*  62 */     this.comment = comment;
/*  63 */     this.localHeaderOffset = localHeaderOffset;
/*     */   }
/*     */ 
/*     */ 
/*     */   
/*     */   void load(byte[] data, int dataOffset, RandomAccessData variableData, int variableOffset, JarEntryFilter filter) throws IOException {
/*  69 */     this.header = data;
/*  70 */     this.headerOffset = dataOffset;
/*  71 */     long nameLength = Bytes.littleEndianValue(data, dataOffset + 28, 2);
/*  72 */     long extraLength = Bytes.littleEndianValue(data, dataOffset + 30, 2);
/*  73 */     long commentLength = Bytes.littleEndianValue(data, dataOffset + 32, 2);
/*  74 */     this.localHeaderOffset = Bytes.littleEndianValue(data, dataOffset + 42, 4);
/*     */     
/*  76 */     dataOffset += 46;
/*  77 */     if (variableData != null) {
/*  78 */       data = variableData.read((variableOffset + 46), nameLength + extraLength + commentLength);
/*     */       
/*  80 */       dataOffset = 0;
/*     */     } 
/*  82 */     this.name = new AsciiBytes(data, dataOffset, (int)nameLength);
/*  83 */     if (filter != null) {
/*  84 */       this.name = filter.apply(this.name);
/*     */     }
/*  86 */     this.extra = NO_EXTRA;
/*  87 */     this.comment = NO_COMMENT;
/*  88 */     if (extraLength > 0L) {
/*  89 */       this.extra = new byte[(int)extraLength];
/*  90 */       System.arraycopy(data, (int)(dataOffset + nameLength), this.extra, 0, this.extra.length);
/*     */     } 
/*     */     
/*  93 */     if (commentLength > 0L) {
/*  94 */       this.comment = new AsciiBytes(data, (int)(dataOffset + nameLength + extraLength), (int)commentLength);
/*     */     }
/*     */   }
/*     */ 
/*     */   
/*     */   public AsciiBytes getName() {
/* 100 */     return this.name;
/*     */   }
/*     */ 
/*     */   
/*     */   public boolean hasName(CharSequence name, char suffix) {
/* 105 */     return this.name.matches(name, suffix);
/*     */   }
/*     */   
/*     */   public boolean isDirectory() {
/* 109 */     return this.name.endsWith(SLASH);
/*     */   }
/*     */ 
/*     */   
/*     */   public int getMethod() {
/* 114 */     return (int)Bytes.littleEndianValue(this.header, this.headerOffset + 10, 2);
/*     */   }
/*     */   
/*     */   public long getTime() {
/* 118 */     long datetime = Bytes.littleEndianValue(this.header, this.headerOffset + 12, 4);
/* 119 */     return decodeMsDosFormatDateTime(datetime);
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   private long decodeMsDosFormatDateTime(long datetime) {
/* 130 */     LocalDateTime localDateTime = LocalDateTime.of((int)((datetime >> 25L & 0x7FL) + 1980L), (int)(datetime >> 21L & 0xFL), (int)(datetime >> 16L & 0x1FL), (int)(datetime >> 11L & 0x1FL), (int)(datetime >> 5L & 0x3FL), (int)(datetime << 1L & 0x3EL));
/*     */ 
/*     */ 
/*     */     
/* 134 */     return localDateTime.toEpochSecond(
/* 135 */         ZoneId.systemDefault().getRules().getOffset(localDateTime)) * 1000L;
/*     */   }
/*     */   
/*     */   public long getCrc() {
/* 139 */     return Bytes.littleEndianValue(this.header, this.headerOffset + 16, 4);
/*     */   }
/*     */ 
/*     */   
/*     */   public long getCompressedSize() {
/* 144 */     return Bytes.littleEndianValue(this.header, this.headerOffset + 20, 4);
/*     */   }
/*     */ 
/*     */   
/*     */   public long getSize() {
/* 149 */     return Bytes.littleEndianValue(this.header, this.headerOffset + 24, 4);
/*     */   }
/*     */   
/*     */   public byte[] getExtra() {
/* 153 */     return this.extra;
/*     */   }
/*     */   
/*     */   public AsciiBytes getComment() {
/* 157 */     return this.comment;
/*     */   }
/*     */ 
/*     */   
/*     */   public long getLocalHeaderOffset() {
/* 162 */     return this.localHeaderOffset;
/*     */   }
/*     */ 
/*     */   
/*     */   public CentralDirectoryFileHeader clone() {
/* 167 */     byte[] header = new byte[46];
/* 168 */     System.arraycopy(this.header, this.headerOffset, header, 0, header.length);
/* 169 */     return new CentralDirectoryFileHeader(header, 0, this.name, header, this.comment, this.localHeaderOffset);
/*     */   }
/*     */ 
/*     */ 
/*     */   
/*     */   public static CentralDirectoryFileHeader fromRandomAccessData(RandomAccessData data, int offset, JarEntryFilter filter) throws IOException {
/* 175 */     CentralDirectoryFileHeader fileHeader = new CentralDirectoryFileHeader();
/* 176 */     byte[] bytes = data.read(offset, 46L);
/* 177 */     fileHeader.load(bytes, 0, data, offset, filter);
/* 178 */     return fileHeader;
/*     */   }
/*     */ }


/* Location:              C:\Users\zhr_0\Desktop\challenge-0.0.1-SNAPSHOT.jar!\org\springframework\boot\loader\jar\CentralDirectoryFileHeader.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */