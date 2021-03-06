package com.ims.security.controller;

import com.ims.common.service.Interface.PeopleManagement;
import com.ims.common.util.Response;
import com.ims.domain.Admin;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/Account")
@ResponseBody
@CrossOrigin
public class AccountController {

    @Autowired
    PeopleManagement peopleManagement;

    @RequestMapping(value = "/Login", method = RequestMethod.POST)
    public String login(String username, String password){
        Subject subject = SecurityUtils.getSubject();
        Response response = Response.generateResponse();
        Admin admin;
        try{
            admin = peopleManagement._selectByUsername(username);
        }catch (IndexOutOfBoundsException e){
            e.printStackTrace();
            response.exception("用户名错误");
            return response.toJSONString();
        }
        if(subject != null){
            if(!subject.isAuthenticated()) {
                UsernamePasswordToken token = new UsernamePasswordToken(username, password);
                try {
                    subject.login(token);
                    response.success();
                    Session session = subject.getSession();
                    session.setAttribute("storehouseId", admin.getStorehouseId());
                    session.setAttribute("userId", admin.getId());
                    session.setAttribute("username", admin.getName());
                    session.setAttribute("password", admin.getPassword());
                    session.setAttribute("modifyPasswordTimes", 0);
                } catch (Exception e) {
                    e.printStackTrace();
                    response.exception("账号或密码错误");
                }
            }
            else{
                response.exception("已登录，当前登录用户为" + admin.getName());
            }
        }
        else{
            response.exception("登录出错");
        }
        return response.toJSONString();
    }

    @RequestMapping(value = "/Logout", method = RequestMethod.POST)
    public String Logout(){
        Response response = Response.generateResponse();
        Subject subject = SecurityUtils.getSubject();
        if(subject != null && subject.isAuthenticated()){
            subject.logout();
            response.success();
        }
        else {
            response.exception("当前未登录");
        }
        return response.toJSONString();
    }

    @RequestMapping(value = "/ModifyPassword", method = RequestMethod.POST)
    public String modifyPassword(String oldPassword, String newPassword){
        Subject subject = SecurityUtils.getSubject();
        Session session = subject.getSession();
        Response response = Response.generateResponse();
        Admin admin = new Admin();
        if(!subject.isAuthenticated()){
            response.exception("未登录");
            return response.toJSONString();
        }
        int retryTimes = Integer.parseInt(session.getAttribute("modifyPasswordTimes").toString());
        if(retryTimes > 10){
            response.exception("尝试修改密码次数过多，请联系管理员修改");
            subject.logout();
            return response.toJSONString();
        }
        if(oldPassword.equals(newPassword)){
            response.exception("新旧密码一致， 无需更改");
            retryTimes++;
            session.setAttribute("modifyPasswordTimes", retryTimes);
            return response.toJSONString();
        }
        if(session.getAttribute("password").toString().equals(oldPassword)) {
            admin.setPassword(newPassword);
            admin.setId(Integer.valueOf(session.getAttribute("userId").toString()));
            try {
                peopleManagement.modifyAdmin(admin);
                response.success();
            } catch (Exception e) {
                response.exception("修改密码失败");
            }
        }
        else{
            retryTimes++;
            session.setAttribute("modifyPasswordTimes", retryTimes);
            response.exception("原密码错误，如果忘记请找管理员修改");
        }
        return response.toJSONString();
    }
}
