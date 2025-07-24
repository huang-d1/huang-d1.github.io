import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class ice{
    public static void main(String[] args)
    {
        String key="c0dehack1nghere1";
        String initVector="0123456789abcdef";
        String value="admin";
        /*    */     try {
        /* 15 */       IvParameterSpec iv = new IvParameterSpec(initVector.getBytes("UTF-8"));
        /* 16 */       SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");
        /*    */
        /* 18 */       Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
        /* 19 */       cipher.init(1, skeySpec, iv);
        /*    */
        /* 21 */       byte[] encrypted = cipher.doFinal(value.getBytes());
        /*    */
        /* 23 */       System.out.println(Base64.getUrlEncoder().encodeToString(encrypted));
        /* 24 */     } catch (Exception ex) {
        /* 28 */       System.out.println(ex.getMessage());
        /*    */     }
    }
}
