package sterbenj.com.simplenotification;

import org.litepal.crud.LitePalSupport;

import java.io.Serializable;

/**
 * XJB Created by 野良人 on 2018/12/5.
 */
public class CircleTip extends LitePalSupport implements Serializable {
    private long id;
    private long TipId;
    private long SpaceTime;
    private int Times;
    private String Requests;//用:分开

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getTipId() {
        return TipId;
    }

    public void setTipId(long tipId) {
        TipId = tipId;
    }

    public long getSpaceTime() {
        return SpaceTime;
    }

    public void setSpaceTime(long spaceTime) {
        SpaceTime = spaceTime;
    }

    public int getTimes() {
        return Times;
    }

    public void setTimes(int times) {
        Times = times;
    }

    public String getRequests() {
        return Requests;
    }

    public void setRequests(String requests) {
        Requests = requests;
    }
}
