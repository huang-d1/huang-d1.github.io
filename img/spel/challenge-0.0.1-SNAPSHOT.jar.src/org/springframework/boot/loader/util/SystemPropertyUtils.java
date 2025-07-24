/*     */ package org.springframework.boot.loader.util;
/*     */ 
/*     */ import java.util.HashSet;
/*     */ import java.util.Locale;
/*     */ import java.util.Properties;
/*     */ import java.util.Set;
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
/*     */ public abstract class SystemPropertyUtils
/*     */ {
/*     */   public static final String PLACEHOLDER_PREFIX = "${";
/*     */   public static final String PLACEHOLDER_SUFFIX = "}";
/*     */   public static final String VALUE_SEPARATOR = ":";
/*  55 */   private static final String SIMPLE_PREFIX = "${".substring(1);
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
/*     */   public static String resolvePlaceholders(String text) {
/*  67 */     if (text == null) {
/*  68 */       return text;
/*     */     }
/*  70 */     return parseStringValue(null, text, text, new HashSet<>());
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
/*     */   public static String resolvePlaceholders(Properties properties, String text) {
/*  84 */     if (text == null) {
/*  85 */       return text;
/*     */     }
/*  87 */     return parseStringValue(properties, text, text, new HashSet<>());
/*     */   }
/*     */ 
/*     */ 
/*     */   
/*     */   private static String parseStringValue(Properties properties, String value, String current, Set<String> visitedPlaceholders) {
/*  93 */     StringBuilder buf = new StringBuilder(current);
/*     */     
/*  95 */     int startIndex = current.indexOf("${");
/*  96 */     while (startIndex != -1) {
/*  97 */       int endIndex = findPlaceholderEndIndex(buf, startIndex);
/*  98 */       if (endIndex != -1) {
/*     */         
/* 100 */         String placeholder = buf.substring(startIndex + "${".length(), endIndex);
/* 101 */         String originalPlaceholder = placeholder;
/* 102 */         if (!visitedPlaceholders.add(originalPlaceholder)) {
/* 103 */           throw new IllegalArgumentException("Circular placeholder reference '" + originalPlaceholder + "' in property definitions");
/*     */         }
/*     */ 
/*     */ 
/*     */ 
/*     */         
/* 109 */         placeholder = parseStringValue(properties, value, placeholder, visitedPlaceholders);
/*     */ 
/*     */         
/* 112 */         String propVal = resolvePlaceholder(properties, value, placeholder);
/* 113 */         if (propVal == null && ":" != null) {
/* 114 */           int separatorIndex = placeholder.indexOf(":");
/* 115 */           if (separatorIndex != -1) {
/* 116 */             String actualPlaceholder = placeholder.substring(0, separatorIndex);
/*     */ 
/*     */             
/* 119 */             String defaultValue = placeholder.substring(separatorIndex + ":".length());
/* 120 */             propVal = resolvePlaceholder(properties, value, actualPlaceholder);
/*     */             
/* 122 */             if (propVal == null) {
/* 123 */               propVal = defaultValue;
/*     */             }
/*     */           } 
/*     */         } 
/* 127 */         if (propVal != null) {
/*     */ 
/*     */           
/* 130 */           propVal = parseStringValue(properties, value, propVal, visitedPlaceholders);
/*     */           
/* 132 */           buf.replace(startIndex, endIndex + "}".length(), propVal);
/*     */           
/* 134 */           startIndex = buf.indexOf("${", startIndex + propVal
/* 135 */               .length());
/*     */         }
/*     */         else {
/*     */           
/* 139 */           startIndex = buf.indexOf("${", endIndex + "}"
/* 140 */               .length());
/*     */         } 
/* 142 */         visitedPlaceholders.remove(originalPlaceholder);
/*     */         continue;
/*     */       } 
/* 145 */       startIndex = -1;
/*     */     } 
/*     */ 
/*     */     
/* 149 */     return buf.toString();
/*     */   }
/*     */ 
/*     */   
/*     */   private static String resolvePlaceholder(Properties properties, String text, String placeholderName) {
/* 154 */     String propVal = getProperty(placeholderName, null, text);
/* 155 */     if (propVal != null) {
/* 156 */       return propVal;
/*     */     }
/* 158 */     return (properties != null) ? properties.getProperty(placeholderName) : null;
/*     */   }
/*     */   
/*     */   public static String getProperty(String key) {
/* 162 */     return getProperty(key, null, "");
/*     */   }
/*     */   
/*     */   public static String getProperty(String key, String defaultValue) {
/* 166 */     return getProperty(key, defaultValue, "");
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
/*     */   public static String getProperty(String key, String defaultValue, String text) {
/*     */     try {
/* 181 */       String propVal = System.getProperty(key);
/* 182 */       if (propVal == null)
/*     */       {
/* 184 */         propVal = System.getenv(key);
/*     */       }
/* 186 */       if (propVal == null) {
/*     */         
/* 188 */         String name = key.replace('.', '_');
/* 189 */         propVal = System.getenv(name);
/*     */       } 
/* 191 */       if (propVal == null) {
/*     */         
/* 193 */         String name = key.toUpperCase(Locale.ENGLISH).replace('.', '_');
/* 194 */         propVal = System.getenv(name);
/*     */       } 
/* 196 */       if (propVal != null) {
/* 197 */         return propVal;
/*     */       }
/*     */     }
/* 200 */     catch (Throwable ex) {
/* 201 */       System.err.println("Could not resolve key '" + key + "' in '" + text + "' as system property or in environment: " + ex);
/*     */     } 
/*     */     
/* 204 */     return defaultValue;
/*     */   }
/*     */   
/*     */   private static int findPlaceholderEndIndex(CharSequence buf, int startIndex) {
/* 208 */     int index = startIndex + "${".length();
/* 209 */     int withinNestedPlaceholder = 0;
/* 210 */     while (index < buf.length()) {
/* 211 */       if (substringMatch(buf, index, "}")) {
/* 212 */         if (withinNestedPlaceholder > 0) {
/* 213 */           withinNestedPlaceholder--;
/* 214 */           index += "}".length();
/*     */           continue;
/*     */         } 
/* 217 */         return index;
/*     */       } 
/*     */       
/* 220 */       if (substringMatch(buf, index, SIMPLE_PREFIX)) {
/* 221 */         withinNestedPlaceholder++;
/* 222 */         index += SIMPLE_PREFIX.length();
/*     */         continue;
/*     */       } 
/* 225 */       index++;
/*     */     } 
/*     */     
/* 228 */     return -1;
/*     */   }
/*     */ 
/*     */   
/*     */   private static boolean substringMatch(CharSequence str, int index, CharSequence substring) {
/* 233 */     for (int j = 0; j < substring.length(); j++) {
/* 234 */       int i = index + j;
/* 235 */       if (i >= str.length() || str.charAt(i) != substring.charAt(j)) {
/* 236 */         return false;
/*     */       }
/*     */     } 
/* 239 */     return true;
/*     */   }
/*     */ }


/* Location:              C:\Users\zhr_0\Desktop\challenge-0.0.1-SNAPSHOT.jar!\org\springframework\boot\loade\\util\SystemPropertyUtils.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */