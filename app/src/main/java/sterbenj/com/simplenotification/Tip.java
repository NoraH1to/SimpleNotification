package sterbenj.com.simplenotification;

import org.litepal.crud.LitePalSupport;

import java.io.Serializable;

/**
 * XJB Created by 野良人 on 2018/10/31.
 */
public class Tip extends LitePalSupport implements Serializable {

    private long id;
    private String Title;
    private String Context;
    private int channel_id;
    private long Time;
    private long TargetTime;
    private int hasNotice;

    public int getHasNotice() {
        return hasNotice;
    }

    public void setHasNotice(int hasNotice) {
        this.hasNotice = hasNotice;
    }

    public long getTargetTime() {
        return TargetTime;
    }

    public void setTargetTime(long targetTime) {
        TargetTime = targetTime;
    }

    public long getTime() {
        return Time;
    }

    public void setTime(long time) {
        Time = time;
    }

    public int getChannel_id() {
        return channel_id;
    }

    public void setChannel_id(int channel_id) {
        this.channel_id = channel_id;
    }

    public String getTitle() {
        return Title;
    }

    public void setTitle(String title) {
        Title = title;
    }

    public String getContext() {
        return Context;
    }

    public void setContext(String context) {
        Context = context;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }
}
