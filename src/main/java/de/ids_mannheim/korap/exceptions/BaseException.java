package de.ids_mannheim.korap.exceptions;

import de.ids_mannheim.korap.auditing.AuditRecord;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author hanl
 * @date 29/01/2014
 */
@Getter
public abstract class BaseException extends Exception {

    protected List<AuditRecord> records = new ArrayList<>();
    private Integer statusCode;
    private String entity;


    public BaseException(int code) {
        this.statusCode = code;
    }

    public BaseException(int status, String message, String entity) {
        super(message);
        this.statusCode = status;
        this.entity = entity;
    }

    public BaseException(int status, String entity) {
        this(status);
        this.entity = entity;
    }

    public BaseException(Throwable cause, int status) {
        super(cause);
        this.statusCode = status;

    }

    public BaseException(String message, Throwable cause, int status) {
        super(message, cause);
        this.statusCode = status;
    }

}
