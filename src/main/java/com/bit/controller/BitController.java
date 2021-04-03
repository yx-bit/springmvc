package com.bit.controller;

import com.bit.anno.Autowired;
import com.bit.anno.Controller;
import com.bit.anno.RequestMapping;
import com.bit.anno.RequestParam;
import com.bit.service.BitService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@Controller
@RequestMapping("/bit")
public class BitController {
    @Autowired("BitService")
    BitService bitService;

    @RequestMapping("/query")
    public void query(HttpServletRequest request, HttpServletResponse response,
    @RequestParam("name")String name,@RequestParam("age")String age){
        try {
            PrintWriter writer = response.getWriter();
            writer.write(bitService.query(name,age));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
