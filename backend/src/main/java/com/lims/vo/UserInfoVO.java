package com.lims.vo;

public class UserInfoVO {

    private String gh;

    private String name;

    private String userRoleId;

    private String deptId;

    public UserInfoVO(String gh, String name, String userRoleId, String deptId) {
        this.gh = gh;
        this.name = name;
        this.userRoleId = userRoleId;
        this.deptId = deptId;
    }

    public String getGh() {
        return gh;
    }

    public void setGh(String gh) {
        this.gh = gh;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUserRoleId() {
        return userRoleId;
    }

    public void setUserRoleId(String userRoleId) {
        this.userRoleId = userRoleId;
    }

    public String getDeptId() {
        return deptId;
    }

    public void setDeptId(String deptId) {
        this.deptId = deptId;
    }
}
