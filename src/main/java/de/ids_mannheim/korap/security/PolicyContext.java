package de.ids_mannheim.korap.security;

import de.ids_mannheim.korap.utils.IPNetMask;
import de.ids_mannheim.korap.utils.TimeUtils;
import lombok.Getter;

import java.net.UnknownHostException;

/**
 * @author hanl
 * @date 09/01/2014
 */
@Getter
public class PolicyContext {

    // refers to a specific ip location
    private String ipmask = "";
    // this context is not like an environmental property (e.g. morning hours/ evening hours), but specifies absolute time
    // parameters (e.g. from 10.04.2014 9:00 till 14..04.2014 active for testing).
    // if the containing parameter do not meet, the policy will be deactivated. if no parameter where specified, the policy
    // remains active
    // specifies a start time for the policy to be activated
    private long start = 0L;
    // specifies a time up to which the policy stays active
    private long end = 0L;


    public PolicyContext() {
        start = TimeUtils.getNow().getMillis();
    }

    public PolicyContext setIPMask(String ip) {
        this.ipmask = ip;
        return this;
    }

    public PolicyContext setExpirationTime(long limit) {
        this.end = limit;
        return this;
    }

    public PolicyContext setEnableTime(long start) {
        this.start = start;
        return this;
    }

    protected boolean isActive(String ipaddress) {
        if (ipaddress == null)
            return false;
        if (noMask())
            return true;
        IPNetMask mask;
        try {
            mask = IPNetMask.getIPMask(this.ipmask);
            boolean f = mask.matches(ipaddress);
            return f;
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return false;
        }
    }

    protected boolean noMask() {
        return ipmask == null || ipmask.isEmpty();
    }

    @Override
    public String toString() {
        return "PolicyContext{" +
                ", ipmask='" + ipmask + '\'' +
                ", start=" + start +
                ", end=" + end +
                '}';
    }
}
