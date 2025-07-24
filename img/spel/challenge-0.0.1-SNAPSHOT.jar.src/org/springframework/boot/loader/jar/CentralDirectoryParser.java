/*     */ package org.springframework.boot.loader.jar;
/*     */ 
/*     */ import java.io.IOException;
/*     */ import java.util.ArrayList;
/*     */ import java.util.List;
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
/*     */ class CentralDirectoryParser
/*     */ {
/*     */   private static final int CENTRAL_DIRECTORY_HEADER_BASE_SIZE = 46;
/*  36 */   private final List<CentralDirectoryVisitor> visitors = new ArrayList<>();
/*     */   
/*     */   public <T extends CentralDirectoryVisitor> T addVisitor(T visitor) {
/*  39 */     this.visitors.add((CentralDirectoryVisitor)visitor);
/*  40 */     return visitor;
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
/*     */   public RandomAccessData parse(RandomAccessData data, boolean skipPrefixBytes) throws IOException {
/*  52 */     CentralDirectoryEndRecord endRecord = new CentralDirectoryEndRecord(data);
/*  53 */     if (skipPrefixBytes) {
/*  54 */       data = getArchiveData(endRecord, data);
/*     */     }
/*  56 */     RandomAccessData centralDirectoryData = endRecord.getCentralDirectory(data);
/*  57 */     visitStart(endRecord, centralDirectoryData);
/*  58 */     parseEntries(endRecord, centralDirectoryData);
/*  59 */     visitEnd();
/*  60 */     return data;
/*     */   }
/*     */ 
/*     */   
/*     */   private void parseEntries(CentralDirectoryEndRecord endRecord, RandomAccessData centralDirectoryData) throws IOException {
/*  65 */     byte[] bytes = centralDirectoryData.read(0L, centralDirectoryData.getSize());
/*  66 */     CentralDirectoryFileHeader fileHeader = new CentralDirectoryFileHeader();
/*  67 */     int dataOffset = 0;
/*  68 */     for (int i = 0; i < endRecord.getNumberOfRecords(); i++) {
/*  69 */       fileHeader.load(bytes, dataOffset, null, 0, null);
/*  70 */       visitFileHeader(dataOffset, fileHeader);
/*  71 */       dataOffset += 46 + fileHeader
/*  72 */         .getName().length() + fileHeader.getComment().length() + (fileHeader
/*  73 */         .getExtra()).length;
/*     */     } 
/*     */   }
/*     */ 
/*     */   
/*     */   private RandomAccessData getArchiveData(CentralDirectoryEndRecord endRecord, RandomAccessData data) {
/*  79 */     long offset = endRecord.getStartOfArchive(data);
/*  80 */     if (offset == 0L) {
/*  81 */       return data;
/*     */     }
/*  83 */     return data.getSubsection(offset, data.getSize() - offset);
/*     */   }
/*     */ 
/*     */   
/*     */   private void visitStart(CentralDirectoryEndRecord endRecord, RandomAccessData centralDirectoryData) {
/*  88 */     for (CentralDirectoryVisitor visitor : this.visitors) {
/*  89 */       visitor.visitStart(endRecord, centralDirectoryData);
/*     */     }
/*     */   }
/*     */   
/*     */   private void visitFileHeader(int dataOffset, CentralDirectoryFileHeader fileHeader) {
/*  94 */     for (CentralDirectoryVisitor visitor : this.visitors) {
/*  95 */       visitor.visitFileHeader(fileHeader, dataOffset);
/*     */     }
/*     */   }
/*     */   
/*     */   private void visitEnd() {
/* 100 */     for (CentralDirectoryVisitor visitor : this.visitors)
/* 101 */       visitor.visitEnd(); 
/*     */   }
/*     */ }


/* Location:              C:\Users\zhr_0\Desktop\challenge-0.0.1-SNAPSHOT.jar!\org\springframework\boot\loader\jar\CentralDirectoryParser.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */