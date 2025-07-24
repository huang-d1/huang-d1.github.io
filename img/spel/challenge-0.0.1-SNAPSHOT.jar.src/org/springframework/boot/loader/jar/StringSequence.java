/*     */ package org.springframework.boot.loader.jar;
/*     */ 
/*     */ import java.util.Objects;
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
/*     */ final class StringSequence
/*     */   implements CharSequence
/*     */ {
/*     */   private final String source;
/*     */   private final int start;
/*     */   private final int end;
/*     */   private int hash;
/*     */   
/*     */   StringSequence(String source) {
/*  39 */     this(source, 0, (source != null) ? source.length() : -1);
/*     */   }
/*     */   
/*     */   StringSequence(String source, int start, int end) {
/*  43 */     Objects.requireNonNull(source, "Source must not be null");
/*  44 */     if (start < 0) {
/*  45 */       throw new StringIndexOutOfBoundsException(start);
/*     */     }
/*  47 */     if (end > source.length()) {
/*  48 */       throw new StringIndexOutOfBoundsException(end);
/*     */     }
/*  50 */     this.source = source;
/*  51 */     this.start = start;
/*  52 */     this.end = end;
/*     */   }
/*     */   
/*     */   public StringSequence subSequence(int start) {
/*  56 */     return subSequence(start, length());
/*     */   }
/*     */ 
/*     */   
/*     */   public StringSequence subSequence(int start, int end) {
/*  61 */     int subSequenceStart = this.start + start;
/*  62 */     int subSequenceEnd = this.start + end;
/*  63 */     if (subSequenceStart > this.end) {
/*  64 */       throw new StringIndexOutOfBoundsException(start);
/*     */     }
/*  66 */     if (subSequenceEnd > this.end) {
/*  67 */       throw new StringIndexOutOfBoundsException(end);
/*     */     }
/*  69 */     return new StringSequence(this.source, subSequenceStart, subSequenceEnd);
/*     */   }
/*     */   
/*     */   public boolean isEmpty() {
/*  73 */     return (length() == 0);
/*     */   }
/*     */ 
/*     */   
/*     */   public int length() {
/*  78 */     return this.end - this.start;
/*     */   }
/*     */ 
/*     */   
/*     */   public char charAt(int index) {
/*  83 */     return this.source.charAt(this.start + index);
/*     */   }
/*     */   
/*     */   public int indexOf(char ch) {
/*  87 */     return this.source.indexOf(ch, this.start) - this.start;
/*     */   }
/*     */   
/*     */   public int indexOf(String str) {
/*  91 */     return this.source.indexOf(str, this.start) - this.start;
/*     */   }
/*     */   
/*     */   public int indexOf(String str, int fromIndex) {
/*  95 */     return this.source.indexOf(str, this.start + fromIndex) - this.start;
/*     */   }
/*     */   
/*     */   public boolean startsWith(CharSequence prefix) {
/*  99 */     return startsWith(prefix, 0);
/*     */   }
/*     */   
/*     */   public boolean startsWith(CharSequence prefix, int offset) {
/* 103 */     if (length() - prefix.length() - offset < 0) {
/* 104 */       return false;
/*     */     }
/* 106 */     return subSequence(offset, offset + prefix.length()).equals(prefix);
/*     */   }
/*     */ 
/*     */   
/*     */   public boolean equals(Object obj) {
/* 111 */     if (this == obj) {
/* 112 */       return true;
/*     */     }
/* 114 */     if (obj == null || !CharSequence.class.isInstance(obj)) {
/* 115 */       return false;
/*     */     }
/* 117 */     CharSequence other = (CharSequence)obj;
/* 118 */     int n = length();
/* 119 */     if (n == other.length()) {
/* 120 */       int i = 0;
/* 121 */       while (n-- != 0) {
/* 122 */         if (charAt(i) != other.charAt(i)) {
/* 123 */           return false;
/*     */         }
/* 125 */         i++;
/*     */       } 
/* 127 */       return true;
/*     */     } 
/* 129 */     return true;
/*     */   }
/*     */ 
/*     */   
/*     */   public int hashCode() {
/* 134 */     int hash = this.hash;
/* 135 */     if (hash == 0 && length() > 0) {
/* 136 */       for (int i = this.start; i < this.end; i++) {
/* 137 */         hash = 31 * hash + this.source.charAt(i);
/*     */       }
/* 139 */       this.hash = hash;
/*     */     } 
/* 141 */     return hash;
/*     */   }
/*     */ 
/*     */   
/*     */   public String toString() {
/* 146 */     return this.source.substring(this.start, this.end);
/*     */   }
/*     */ }


/* Location:              C:\Users\zhr_0\Desktop\challenge-0.0.1-SNAPSHOT.jar!\org\springframework\boot\loader\jar\StringSequence.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */