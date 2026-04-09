// com/capp/client/model/Setting.java
package com.app.client.model;

public class Setting {
    private int    userId;
    private String displayName;
    private String backgroundColor;

    public Setting(int userId, String text, String bgColorFieldText) { }

    // ctor now matches exactly the three columns in your table
    public Setting(int userId, String displayName) {
        this.userId          = userId;
        this.displayName     = displayName;
        this.backgroundColor = "#FFFFFF";
    }



    public int getUserId() {
        return userId;
    }
    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getDisplayName() {
        return displayName;
    }
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getBackgroundColor() {
        return backgroundColor;
    }
    public void setBackgroundColor(String backgroundColor) {
        this.backgroundColor = backgroundColor;
    }


}
