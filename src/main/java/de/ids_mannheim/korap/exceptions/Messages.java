package de.ids_mannheim.korap.exceptions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import de.ids_mannheim.korap.utils.JsonUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author hanl
 * @date 03/12/2014
 */
public class Messages implements Cloneable, Iterable<Message> {

    private ArrayList<Message> messages = new ArrayList(3);

    public Messages() {
    }

    public Iterator<Message> iterator() {
        return new Messages.MessageIterator();
    }

    public Message add(int code, String message, String... terms) {
        Message newMsg = new Message(code, message);
        this.messages.add(newMsg);
        if (terms != null) {
            String[] arr$ = terms;
            int len$ = terms.length;

            for (int i$ = 0; i$ < len$; ++i$) {
                String t = arr$[i$];
                newMsg.addParameter(t);
            }
        }

        return newMsg;
    }

    public Message add(Message msg) {
        try {
            Message e = (Message) msg.clone();
            this.messages.add(e);
            return e;
        } catch (CloneNotSupportedException var3) {
            return (Message) null;
        }
    }

    public Message add(JsonNode msg) throws KorAPException {
        if (msg.isArray() && msg.has(0)) {
            Message newMsg = new Message();
            short i = 1;
            if (msg.get(0).isNumber()) {
                newMsg.setCode(msg.get(0).asInt());
                if (!msg.has(1)) {
                    throw new KorAPException(750, "Passed notifications are not well formed", null);
                }

                newMsg.setMessage(msg.get(1).asText());
                ++i;
            } else {
                newMsg.setMessage(msg.get(0).asText());
            }

            while (msg.has(i)) {
                newMsg.addParameter(msg.get(i++).asText());
            }

            this.add((Message) newMsg);
            return newMsg;
        } else {
            throw new KorAPException(750, "Passed notifications are not well formed", null);
        }
    }

    public Messages add(Messages msgs) {
        try {
            Iterator e = msgs.getMessages().iterator();

            while (e.hasNext()) {
                Message msg = (Message) e.next();
                this.add((Message) ((Message) msg.clone()));
            }
        } catch (CloneNotSupportedException var4) {
            ;
        }

        return this;
    }

    public Messages clear() {
        this.messages.clear();
        return this;
    }

    public int size() {
        return this.messages.size();
    }

    @JsonIgnore
    public Message get(int index) {
        return index >= this.size() ? (Message) null : (Message) this.messages.get(index);
    }

    @JsonIgnore
    public List<Message> getMessages() {
        return this.messages;
    }

    public Object clone() throws CloneNotSupportedException {
        Messages clone = new Messages();
        Iterator i$ = this.messages.iterator();

        while (i$.hasNext()) {
            Message m = (Message) i$.next();
            clone.add((Message) ((Message) m.clone()));
        }

        return clone;
    }

    public JsonNode toJSONnode() {
        ArrayNode messageArray = JsonUtils.createArrayNode();
        Iterator i$ = this.messages.iterator();

        while (i$.hasNext()) {
            Message msg = (Message) i$.next();
            messageArray.add(msg.toJSONnode());
        }

        return messageArray;
    }

    public String toJSON() {
        String msg = "";

        try {
            return JsonUtils.toJSON(this.toJSONnode());
        } catch (Exception var3) {
            msg = ", \"" + var3.getLocalizedMessage() + "\"";
            return "[620, \"Unable to generate JSON\"" + msg + "]";
        }
    }

    private class MessageIterator implements Iterator<Message> {
        int index = 0;

        public MessageIterator() {
        }

        public boolean hasNext() {
            return this.index < Messages.this.messages.size();
        }

        public Message next() {
            return (Message) Messages.this.messages.get(this.index++);
        }

        public void remove() {
            Messages.this.messages.remove(this.index);
        }
    }
}
