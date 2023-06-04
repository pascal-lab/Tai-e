public class CallSiteMode {

    public static void main(String[] args) {
        testConnection(null, null, null); // ensure no call edge
    }

    static void testConnection(Source source, StringBuilder sb, Connection conn) {
        String input = source.getSource();
        sb.append("select * from users where id = '");
        sb.append(input);
        sb.append("'");
        String sql = sb.toString();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sql); // taint
    }
}

// test call site source
interface Source {

    String getSource();
}

// mimic StringBuilder to test call site transfer
interface StringBuilder {

    StringBuilder append(String s);

    String toString();
}

// mimic JDBC to test call site sink
interface Connection {

    Statement createStatement();
}

interface Statement {

    ResultSet executeQuery(String sql);
}

class ResultSet {
}
