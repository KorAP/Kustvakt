package de.ids_mannheim.korap.exceptions;

import lombok.Getter;
import lombok.Setter;

/**
 * @author hanl
 * @date 11/12/2013
 */
//fixme: redundant with baseexception
@Deprecated
@Setter
@Getter
public class KorAPException extends BaseException {

    private String userid;

    public KorAPException(Integer status) {
        super(status);
    }

    public KorAPException(Object userid, Integer status) {
        this(status);
        this.userid = String.valueOf(userid);
    }

    public KorAPException(Object userid, Integer status, String message,
            String entity) {
        super(status, message, entity);
        this.userid = String.valueOf(userid);
    }

    public KorAPException(Integer status, String message, String entity) {
        super(status, message, entity);
    }

    public KorAPException(Throwable cause, Integer status) {
        super(cause, status);
    }

    public KorAPException(String message, Throwable cause, Integer status) {
        super(message, cause, status);
    }

    @Override
    public String toString() {
        return "Excpt{" +
                "status=" + getStatusCode() +
                ", message=" + getMessage() +
                ", args=" + getEntity() +
                ", userid=" + userid +
                '}';
    }
}
