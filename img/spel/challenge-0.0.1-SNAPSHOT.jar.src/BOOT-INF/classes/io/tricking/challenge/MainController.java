/*     */ package BOOT-INF.classes.io.tricking.challenge;
/*     */ import io.tricking.challenge.KeyworkProperties;
/*     */ import io.tricking.challenge.spel.SmallEvaluationContext;
/*     */ import java.util.regex.Matcher;
/*     */ import java.util.regex.Pattern;
/*     */ import javax.servlet.http.Cookie;
/*     */ import javax.servlet.http.HttpServletResponse;
/*     */ import javax.servlet.http.HttpSession;
/*     */ import org.springframework.beans.factory.annotation.Autowired;
/*     */ import org.springframework.expression.EvaluationContext;
/*     */ import org.springframework.expression.Expression;
/*     */ import org.springframework.expression.ExpressionParser;
/*     */ import org.springframework.expression.ParserContext;
/*     */ import org.springframework.expression.common.TemplateParserContext;
/*     */ import org.springframework.http.HttpStatus;
/*     */ import org.springframework.stereotype.Controller;
/*     */ import org.springframework.ui.Model;
/*     */ import org.springframework.web.bind.annotation.GetMapping;
/*     */ import org.springframework.web.bind.annotation.RequestParam;
/*     */ import org.springframework.web.client.HttpClientErrorException;
/*     */ 
/*     */ @Controller
/*     */ public class MainController {
/*  24 */   ExpressionParser parser = (ExpressionParser)new SpelExpressionParser();
/*     */ 
/*     */   
/*     */   @Autowired
/*     */   private KeyworkProperties keyworkProperties;
/*     */   
/*     */   @Autowired
/*     */   private UserConfig userConfig;
/*     */ 
/*     */   
/*     */   @GetMapping
/*     */   public String admin(@CookieValue(value = "remember-me", required = false) String rememberMeValue, HttpSession session, Model model) {
/*  36 */     if (rememberMeValue != null && !rememberMeValue.equals("")) {
/*  37 */       String str = this.userConfig.decryptRememberMe(rememberMeValue);
/*  38 */       if (str != null) {
/*  39 */         session.setAttribute("username", str);
/*     */       }
/*     */     } 
/*     */     
/*  43 */     Object username = session.getAttribute("username");
/*  44 */     if (username == null || username.toString().equals("")) {
/*  45 */       return "redirect:/login";
/*     */     }
/*     */     
/*  48 */     model.addAttribute("name", getAdvanceValue(username.toString()));
/*  49 */     return "hello";
/*     */   }
/*     */   
/*     */   @GetMapping({"/login"})
/*     */   public String login() {
/*  54 */     return "login";
/*     */   }
/*     */   
/*     */   @GetMapping({"/login-error"})
/*     */   public String loginError(Model model) {
/*  59 */     model.addAttribute("loginError", Boolean.valueOf(true));
/*  60 */     model.addAttribute("errorMsg", "登陆失败，用户名或者密码错误！");
/*  61 */     return "login";
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   @PostMapping({"/login"})
/*     */   public String login(@RequestParam(value = "username", required = true) String username, @RequestParam(value = "password", required = true) String password, @RequestParam(value = "remember-me", required = false) String isRemember, HttpSession session, HttpServletResponse response) {
/*  70 */     if (this.userConfig.getUsername().contentEquals(username) && this.userConfig.getPassword().contentEquals(password)) {
/*  71 */       session.setAttribute("username", username);
/*     */       
/*  73 */       if (isRemember != null && !isRemember.equals("")) {
/*  74 */         Cookie c = new Cookie("remember-me", this.userConfig.encryptRememberMe());
/*  75 */         c.setMaxAge(2592000);
/*  76 */         response.addCookie(c);
/*     */       } 
/*     */       
/*  79 */       return "redirect:/";
/*     */     } 
/*  81 */     return "redirect:/login-error";
/*     */   }
/*     */ 
/*     */   
/*     */   @ExceptionHandler({HttpClientErrorException.class})
/*     */   @ResponseStatus(HttpStatus.FORBIDDEN)
/*     */   public String handleForbiddenException() {
/*  88 */     return "forbidden";
/*     */   }
/*     */   
/*     */   private String getAdvanceValue(String val) {
/*  92 */     for (String keyword : this.keyworkProperties.getBlacklist()) {
/*  93 */       Matcher matcher = Pattern.compile(keyword, 34).matcher(val);
/*  94 */       if (matcher.find()) {
/*  95 */         throw new HttpClientErrorException(HttpStatus.FORBIDDEN);
/*     */       }
/*     */     } 
/*     */     
/*  99 */     TemplateParserContext templateParserContext = new TemplateParserContext();
/* 100 */     Expression exp = this.parser.parseExpression(val, (ParserContext)templateParserContext);
/* 101 */     SmallEvaluationContext evaluationContext = new SmallEvaluationContext();
/* 102 */     return exp.getValue((EvaluationContext)evaluationContext).toString();
/*     */   }
/*     */ }


/* Location:              C:\Users\zhr_0\Desktop\challenge-0.0.1-SNAPSHOT.jar!\BOOT-INF\classes\io\tricking\challenge\MainController.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */