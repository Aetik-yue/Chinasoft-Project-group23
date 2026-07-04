import java.sql.*;

public class DbProbe {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://47.108.58.107:3306/dream28?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true";
        String[] tests = {
            "jdbc:mysql://47.108.58.107:3306/dream28?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai",
            "jdbc:mysql://47.108.58.107:3306/?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai",
            "jdbc:mysql://47.108.58.107:3306/mysql?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai"
        };
        for (int i = 0; i < tests.length; i++) {
            System.out.println("=== TEST " + (i+1) + " ===");
            System.out.println("URL: " + tests[i]);
            try (Connection c = DriverManager.getConnection(tests[i], "root", "c0765083cd3f57ab")) {
                System.out.println("CONNECTED ok. catalog=" + c.getCatalog());
                try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT VERSION(), DATABASE()")) {
                    if (rs.next()) System.out.println("  VERSION=" + rs.getString(1) + " DB=" + rs.getString(2));
                }
                try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SHOW DATABASES")) {
                    StringBuilder sb = new StringBuilder("  DATABASES: ");
                    while (rs.next()) sb.append(rs.getString(1)).append(" ");
                    System.out.println(sb);
                }
            } catch (Exception e) {
                System.out.println("FAIL: " + e.getClass().getName() + ": " + e.getMessage());
                Throwable c2 = e.getCause();
                while (c2 != null) { System.out.println("  caused by: " + c2.getClass().getName() + ": " + c2.getMessage()); c2 = c2.getCause(); }
            }
        }
    }
}
