package com.example.app;

import com.acme.arc.dep.test.LowOneMain;
import com.example.library.UserService;
import com.example.midlib.MidLibService;

public class App {
    public static void main(String[] args) {
        System.out.println("I love bazel");
        LowOneMain.say();
        System.out.println("All users: " + new UserService().findAll());
        MidLibService.say();
    }
}
