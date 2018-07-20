import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.*;

public class SAXHandler extends DefaultHandler{

    private String sender, receiver, type, content;

    String getSender(){
        return sender;
    }
    String getReceiver(){
        return receiver;
    }
    String getContent(){
        return content;
    }
    String getType(){
        return type;
    }

    @Override
    public void startElement(String namespaceURI, String localName, String qName, Attributes atts)
    {
        for (int i = 0; i < atts.getLength(); i++)
        {
            switch (atts.getLocalName(i)) {
                case "sender":
                    sender = atts.getValue(i);
                    break;
                case "receiver":
                    receiver = atts.getValue(i);
                    break;
                case "type":
                    type = atts.getValue(i);
                    break;
                case "content":
                    content = atts.getValue(i);
                    break;
                default:
                    break;
            }
        }
    }

    static String getMessage(String mType, String mSender, String mReciever, String mContent)
    {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><message type=\"" + mType + "\" " +
                "sender=\"" + mSender + "\" receiver=\"" + mReciever + "\" content=\"" + mContent + "\"></message>";
    }
}