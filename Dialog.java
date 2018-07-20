import java.util.List;
import java.util.Vector;

class Dialog {

    private String user1;
    private String user2;
    private Vector<String> conversation;

    Dialog(String u1, String u2)
    {
        user1 = u1;
        user2 = u2;
        conversation = new Vector<>();
    }

    void write(String sender, String text)
    {
        String letter = sender + ": " + text + "\n";
        conversation.add(letter);
    }

    boolean members(String u1, String u2)
    {
        return (user1.equals(u1) && user2.equals(u2)) || (user1.equals(u2) && user2.equals(u1));
    }

    boolean member(String u)
    {
        return user1.equals(u) || user2.equals(u);
    }

    String getMembers()
    {
        return user1 + "<-->" + user2;
    }

    String getConverstaion()
    {
        String result = "";
        for(int i = 0; i < conversation.size(); i++)
            result += conversation.get(i);
        return result;
    }
}
